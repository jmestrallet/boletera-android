package uy.boletera.prueba;

import android.app.assist.AssistStructure;
import android.os.CancellationSignal;
import android.service.autofill.*;
import android.view.autofill.AutofillId;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Test APK only; standalone Java because its service process cannot load the target's Kotlin runtime. */
public class FixtureCardAutofillService extends AutofillService {
    @Override public void onFillRequest(FillRequest request, CancellationSignal signal, FillCallback callback) {
        FillContext context = request.getFillContexts().get(request.getFillContexts().size()-1);
        AssistStructure structure = context.getStructure();
        if (!"uy.boletera.prueba".equals(structure.getActivityComponent().getPackageName())) {
            callback.onSuccess(null); return;
        }
        Map<String,AutofillId> fields = new HashMap<>();
        for (int i=0; i<structure.getWindowNodeCount(); i++) visit(structure.getWindowNodeAt(i).getRootViewNode(),fields);
        boolean manual = (request.getFlags() & FillRequest.FLAG_MANUAL_REQUEST) != 0;
        AutofillId card = fields.get("creditCardNumber");
        boolean focusedCard = android.os.Build.VERSION.SDK_INT >= 29 && card != null && card.equals(context.getFocusedId());
        // No text values are read or recorded. This service never returns credentials.
        try (FileWriter out = new FileWriter(new File(getFilesDir(),"card-autofill-proof"),true)) {
            out.write("fields="+fields.size()+";manual="+manual+";focusedCard="+focusedCard+"\n");
        } catch (IOException exception) { callback.onFailure("Could not write test metadata"); return; }
        callback.onSuccess(null);
    }
    private void visit(AssistStructure.ViewNode node, Map<String,AutofillId> fields) {
        if (node.getAutofillHints()!=null && node.getAutofillId()!=null) {
            for (String hint:node.getAutofillHints()) {
                if (hint.equals("creditCardNumber") || hint.equals("creditCardExpirationDate") || hint.equals("creditCardSecurityCode"))
                    fields.put(hint,node.getAutofillId());
            }
        }
        for (int i=0;i<node.getChildCount();i++) visit(node.getChildAt(i),fields);
    }
    @Override public void onSaveRequest(SaveRequest request, SaveCallback callback) { callback.onSuccess(); }
}
