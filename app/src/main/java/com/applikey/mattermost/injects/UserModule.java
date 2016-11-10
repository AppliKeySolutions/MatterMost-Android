package com.applikey.mattermost.injects;

import android.content.Context;

import com.applikey.mattermost.Constants;
import com.applikey.mattermost.manager.metadata.MetaDataManager;
import com.applikey.mattermost.storage.db.ChannelStorage;
import com.applikey.mattermost.storage.db.Db;
import com.applikey.mattermost.storage.db.PostStorage;
import com.applikey.mattermost.storage.db.UserStorage;
import com.applikey.mattermost.storage.preferences.PersistencePrefs;
import com.applikey.mattermost.storage.preferences.Prefs;
import com.applikey.mattermost.utils.image.ImagePathHelper;
import com.google.gson.Gson;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
@PerUser
public class UserModule {

    @Provides
    @PerUser
    @Named(Constants.CURRENT_USER_QUALIFIER)
    String provideCurrentUserId(Prefs mPrefs) {
        return mPrefs.getCurrentUserId();
    }

    @Provides
    @PerUser
    PersistencePrefs providePersistencePrefs(Context context, Gson gson) {
        return new PersistencePrefs(context, gson);
    }

    @Provides
    @PerUser
    ChannelStorage provideChannelStorage(Db db, Prefs prefs, MetaDataManager metaDataManager) {
        return new ChannelStorage(db, prefs, metaDataManager);
    }

    @Provides
    @PerUser
    UserStorage provideUserStorage(Db db, ImagePathHelper imagePathHelper) {
        return new UserStorage(db, imagePathHelper);
    }

    @Provides
    @PerUser
    PostStorage providePostStorage(Db db) {
        return new PostStorage(db);
    }

    @Provides
    @PerUser
    MetaDataManager provideMetadataManager(Prefs prefs, PersistencePrefs persistencePrefs) {
        return new MetaDataManager(prefs, persistencePrefs);
    }
}
