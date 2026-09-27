package tv.mimo.app.mobile;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import tv.mimo.app.R;

public class AboutFragment extends MobileBaseFragment {

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_about;
    }

    @Override
    public void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView version = findView(view, R.id.about_version);
        version.setText(getString(R.string.about_version));
    }
}