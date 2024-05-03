package si.maev.twa;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.androidbrowserhelper.trusted.QualityEnforcer;

public class ValidatedQualityEnforcer extends QualityEnforcer {
    @Nullable
    private final Runnable mOnRelationValidatedRunnable;

    public ValidatedQualityEnforcer(@Nullable Runnable onRelationValidatedRunnable) {
        mOnRelationValidatedRunnable = onRelationValidatedRunnable;
    }

    @Override
    public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin, boolean result, @Nullable Bundle extras) {
        super.onRelationshipValidationResult(relation, requestedOrigin, result, extras);

        if (result) {
            Log.d(this.getClass().getSimpleName(), "Validation result: " + result);
        } else {
            Log.e(this.getClass().getSimpleName(), "Validation result: " + result);
        }

        if (mOnRelationValidatedRunnable != null) {
            mOnRelationValidatedRunnable.run();
        }
    }
}
