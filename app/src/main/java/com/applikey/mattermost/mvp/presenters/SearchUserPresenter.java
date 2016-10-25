package com.applikey.mattermost.mvp.presenters;

import android.util.Log;

import com.applikey.mattermost.App;
import com.applikey.mattermost.models.channel.DirectChannelRequest;
import com.applikey.mattermost.models.user.User;
import com.applikey.mattermost.mvp.views.SearchUserView;
import com.applikey.mattermost.storage.db.ChannelStorage;
import com.applikey.mattermost.storage.db.ObjectNotFoundException;
import com.applikey.mattermost.storage.db.TeamStorage;
import com.applikey.mattermost.storage.db.UserStorage;
import com.applikey.mattermost.storage.preferences.Prefs;
import com.applikey.mattermost.web.Api;
import com.applikey.mattermost.web.ErrorHandler;
import com.arellomobile.mvp.InjectViewState;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author Anatoliy Chub
 */
@InjectViewState
public class SearchUserPresenter extends BasePresenter<SearchUserView> {

    private static final String TAG = SearchUserPresenter.class.getSimpleName();

    @Inject
    UserStorage mUserStorage;

    @Inject
    ChannelStorage mChannelStorage;

    @Inject
    Api mApi;

    @Inject
    Prefs mPrefs;

    @Inject
    TeamStorage mTeamStorage;

    public SearchUserPresenter() {
        App.getComponent().inject(this);
    }

    public void getData(String text) {
        final SearchUserView view = getViewState();
        mSubscription.add(
                mUserStorage.searchUsers(text)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view::displayData, ErrorHandler::handleError));
    }

    public void handleUserClick(User user) {
        final SearchUserView view = getViewState();
        Log.d(TAG, "handleUserClick: " + user.getId());
        mSubscription.add(mChannelStorage.getChannel(user.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::startChatActivity, throwable -> {
                    if (throwable instanceof ObjectNotFoundException) {
                        Log.d(TAG, "handleUserClick: object not find");
                        createChannel(user);
                    }
                }));
    }

    private void createChannel( User user) {
        final SearchUserView view = getViewState();
        view.showLoading(true);
        mTeamStorage.getChosenTeam()
                .observeOn(Schedulers.io())
                .flatMap(team -> mApi.createChannel(team.getId(), new DirectChannelRequest(user.getId())),
                        (team, channel) -> channel)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(channel -> mChannelStorage.saveChannel(channel))
                .subscribe(channel -> {
                    view.startChatActivity(channel);
                    view.showLoading(false);
                    Log.d(TAG, "createChannel: " + channel);
                }, throwable -> {
                    throwable.printStackTrace();
                    view.showLoading(false);
                });

    }

}
