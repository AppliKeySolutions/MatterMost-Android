package com.applikey.mattermost.injects;

import android.content.Context;
import android.support.v4.app.NotificationManagerCompat;

import com.applikey.mattermost.App;
import com.applikey.mattermost.Constants;
import com.applikey.mattermost.activities.ChatActivity;
import com.applikey.mattermost.activities.ChatListActivity;
import com.applikey.mattermost.manager.RxForeground;
import com.applikey.mattermost.storage.db.Db;
import com.applikey.mattermost.storage.db.TeamStorage;
import com.applikey.mattermost.storage.preferences.Prefs;
import com.applikey.mattermost.utils.image.ImagePathHelper;
import com.applikey.mattermost.web.Api;
import com.applikey.mattermost.web.ApiDelegate;
import com.applikey.mattermost.web.BearerTokenFactory;
import com.applikey.mattermost.web.ServerUrlFactory;
import com.applikey.mattermost.web.images.ImageLoader;
import com.applikey.mattermost.web.images.PicassoImageLoader;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import rx.Observable;

@Module
public class GlobalModule {

    private final Context mApplicationContext;

    public GlobalModule(App app) {
        mApplicationContext = app;
    }

    @Provides
    @PerApp
    EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @PerApp
    Realm provideRealm() {
        final RealmConfiguration config = new RealmConfiguration.Builder(mApplicationContext)
                .name(Constants.REALM_NAME)
                .schemaVersion(0)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(config);
        return Realm.getInstance(config);
    }

    @Provides
    @PerApp
    ImageLoader provideImageLoader(OkHttpClient client) {
        return new PicassoImageLoader(mApplicationContext, client);
    }

    @Provides
    @PerApp
    Prefs providePrefs() {
        return new Prefs(mApplicationContext);
    }

    @Provides
    BearerTokenFactory provideTokenFactory(Prefs prefs) {
        return new BearerTokenFactory(prefs);
    }

    @Provides
    @PerApp
    ServerUrlFactory provideServerUrlFactory(Prefs prefs) {
        return new ServerUrlFactory(prefs);
    }

    @Provides
    @PerApp
    OkHttpClient provideOkHttpClient(BearerTokenFactory tokenFactory) {
        final OkHttpClient.Builder okClientBuilder = new OkHttpClient.Builder();
        okClientBuilder.addNetworkInterceptor(new StethoInterceptor());
        okClientBuilder.addInterceptor(chain -> {
            Request request = chain.request();
            final String authToken = tokenFactory.getBearerTokenString();
            if (authToken != null) {
                final Headers headers = request.headers()
                        .newBuilder()
                        .add(Constants.AUTHORIZATION_HEADER, authToken)
                        .build();
                request = request.newBuilder().headers(headers).build();
            }
            return chain.proceed(request);
        });
        final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        okClientBuilder.addInterceptor(httpLoggingInterceptor);
        final File baseDir = mApplicationContext.getCacheDir();
        if (baseDir != null) {
            final File cacheDir = new File(baseDir, "HttpResponseCache");
            okClientBuilder.cache(new Cache(cacheDir, 1024 * 1024 * 50));
        }
        okClientBuilder.connectTimeout(Constants.TIMEOUT_DURATION_SEC, TimeUnit.SECONDS);
        okClientBuilder.readTimeout(Constants.TIMEOUT_DURATION_SEC, TimeUnit.SECONDS);
        okClientBuilder.writeTimeout(Constants.TIMEOUT_DURATION_SEC, TimeUnit.SECONDS);
        return okClientBuilder.build();
    }

    @Provides
    @PerApp
    Api provideApi(OkHttpClient okHttpClient, ServerUrlFactory serverUrlFactory) {
        return new ApiDelegate(okHttpClient, serverUrlFactory);
    }

    @Provides
    @PerApp
    NotificationManagerCompat provideNotificationManager() {
        return NotificationManagerCompat.from(mApplicationContext);
    }

    @Provides
    @PerApp
    Db provideDb(Realm realm) {
        return new Db(realm);
    }

    @Provides
    @PerApp
    TeamStorage provideTeamStorage(Db db) {
        return new TeamStorage(db);
    }

    @Provides
    @PerApp
    ImagePathHelper provideImagePathHelper(ServerUrlFactory serverUrlFactory) {
        return new ImagePathHelper(serverUrlFactory);
    }

    @Provides
    @PerApp
    Context provideApplicationContext() {
        return mApplicationContext;
    }

    @Provides
    @PerApp
    RxForeground provideForegroundManager(Context context) {
        return RxForeground.with(context);
    }

    @Provides
    @PerApp
    Gson provideGson() {
        return new Gson();
    }
}
