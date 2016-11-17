package com.applikey.mattermost.mvp.presenters;

import android.text.TextUtils;

import com.applikey.mattermost.App;
import com.applikey.mattermost.Constants;
import com.applikey.mattermost.events.SearchUserTextChanged;
import com.applikey.mattermost.models.channel.DirectChannelRequest;
import com.applikey.mattermost.models.user.User;
import com.applikey.mattermost.mvp.views.SearchUserView;
import com.applikey.mattermost.mvp.views.SearchView;
import com.arellomobile.mvp.InjectViewState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@InjectViewState
public class SearchUserPresenter extends SearchPresenter<SearchUserView> {

    private static final String TAG = SearchUserPresenter.class.getSimpleName();

    @Inject
    EventBus mEventBus;

    @Inject
    @Named(Constants.CURRENT_USER_QUALIFIER)
    String mCurrentUserId;

    public SearchUserPresenter() {
        App.getUserComponent().inject(this);
        mEventBus.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Override
    public boolean isDataRequestValid(String text) {
        return !TextUtils.isEmpty(text);
    }

    @Override
    public void doRequest(SearchView view, String text) {
        mSubscription.add(
                mUserStorage.searchUsers(text)
                        .first()
                        .flatMap(Observable::from)
                        .filter(user -> !TextUtils.equals(user.getId(), mCurrentUserId))
                        .toList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(users ->  view.displayData(new ArrayList<>(users)), mErrorHandler::handleError));
    }

    @Subscribe
    public void onInputTextChanged(SearchUserTextChanged event) {
        final SearchUserView view = getViewState();
        view.clearData();
        getData(event.getText());
    }

    void createChannel(User user) {
        final SearchUserView view = getViewState();
        view.showLoading(true);
        final Subscription subscription = mTeamStorage.getChosenTeam()
                .observeOn(Schedulers.io())
                .flatMap(team -> mApi.createChannel(team.getId(),
                                                    new DirectChannelRequest(user.getId())),
                         (team, channel) -> channel)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(channel -> mChannelStorage.saveChannel(channel))
                .subscribe(channel -> {
                    view.startChatView(channel);
                    view.showLoading(false);
                }, throwable -> {
                    throwable.printStackTrace();
                    view.showLoading(false);
                });

        mSubscription.add(subscription);
    }
}
