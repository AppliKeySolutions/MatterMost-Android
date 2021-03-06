package com.applikey.mattermost.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.applikey.mattermost.gcm.RegistrationIntentService;
import com.applikey.mattermost.manager.notitifcation.NotificationManager;
import com.applikey.mattermost.mvp.presenters.SplashPresenter;
import com.applikey.mattermost.mvp.views.SplashView;
import com.arellomobile.mvp.presenter.InjectPresenter;

public class SplashActivity extends BaseMvpActivity implements SplashView {

    @InjectPresenter
    SplashPresenter mPresenter;

    public static Intent getIntent(Context context, Bundle bundle) {
        final Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(NotificationManager.NOTIFICATION_BUNDLE_KEY, bundle);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(RegistrationIntentService.getIntent(this));
    }

    @Override
    public void isSessionExist(boolean exist) {
        final Intent intent;
        if (exist) {
            intent = ChatListActivity.getIntent(this,
                    getIntent().getBundleExtra(NotificationManager.NOTIFICATION_BUNDLE_KEY));
        } else {
            intent = new Intent(this, ChooseServerActivity.class);
        }
        startActivity(intent);
    }
}
