package com.applikey.mattermost.mvp.presenters;

import android.text.TextUtils;

import com.applikey.mattermost.App;
import com.applikey.mattermost.Constants;
import com.applikey.mattermost.events.SearchAllTextChanged;
import com.applikey.mattermost.models.SearchItem;
import com.applikey.mattermost.models.channel.Channel;
import com.applikey.mattermost.models.user.User;
import com.applikey.mattermost.mvp.views.SearchAllView;
import com.applikey.mattermost.mvp.views.SearchView;
import com.applikey.mattermost.utils.MessageDateComparator;
import com.arellomobile.mvp.InjectViewState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@InjectViewState
public class SearchAllPresenter extends SearchPresenter<SearchAllView> {

    private static final String TAG = SearchAllPresenter.class.getSimpleName();

    @Inject
    EventBus mEventBus;

    @Inject
    @Named(Constants.CURRENT_USER_QUALIFIER)
    String mCurrentUserId;

    public SearchAllPresenter() {
        App.getUserComponent().inject(this);
        mEventBus.register(this);
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    @Override
    public boolean isDataRequestValid(String text) {
        return mChannelsIsFetched && !TextUtils.isEmpty(text);
    }

    @Override
    public void doRequest(SearchView view, String text) {
        mSubscription.clear();
        mSubscription.add(
                Observable.zip(
                        Channel.getList(mChannelStorage.listUndirected(text))
                                .first(),

                        mUserStorage.searchUsers(text).first(), (items, users) -> {
                            final List<SearchItem> searchItemList = new ArrayList<>();
                            for(Channel channel : items) {
                                 searchItemList.add(new SearchItem(channel));
                            }
                            for(User user : users) {
                                searchItemList.add(new SearchItem(user));
                            }
                            return searchItemList;
                        })
                        .flatMap(items -> getPostsObservable(text).first(),
                                 (items, items2) -> {
                                     items.addAll(items2);
                                     return items;
                                 })
                        .doOnNext(items -> Collections.sort(items, new MessageDateComparator()))
                        .debounce(Constants.INPUT_REQUEST_TIMEOUT_MILLISEC, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view::displayData, mErrorHandler::handleError));
    }

    @Subscribe
    public void onInputTextChanged(SearchAllTextChanged event) {
        super.onInputTextChanged(event);
    }

}
