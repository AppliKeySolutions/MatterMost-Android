package com.applikey.mattermost.mvp.presenters;

import android.text.TextUtils;
import android.util.Log;

import com.applikey.mattermost.App;
import com.applikey.mattermost.Constants;
import com.applikey.mattermost.events.SearchChannelTextChanged;
import com.applikey.mattermost.models.SearchItem;
import com.applikey.mattermost.models.channel.Channel;
import com.applikey.mattermost.mvp.views.SearchChannelView;
import com.applikey.mattermost.mvp.views.SearchView;
import com.applikey.mattermost.utils.ChannelDateComparator;
import com.arellomobile.mvp.InjectViewState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;

@InjectViewState
public class SearchChannelPresenter extends SearchPresenter<SearchChannelView> {

    private static final String TAG = SearchChannelPresenter.class.getSimpleName();

    @Inject
    EventBus mEventBus;

    @Inject
    @Named(Constants.CURRENT_USER_QUALIFIER)
    String mCurrentUserId;

    public SearchChannelPresenter() {
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
        return mChannelsIsFetched  && !TextUtils.isEmpty(text);
    }

    @Override
    public void doRequest(SearchView view, String text) {
        mSubscription.clear();
        mSubscription.add(
                mChannelStorage.listUndirected(text)
                        .first()
                        .map(Channel::getList)
                        .doOnNext(channels -> Log.d(TAG, "doRequest: " + channels))
                        .doOnNext(items -> Collections.sort(items, new ChannelDateComparator()))
                        .flatMap(Observable::from)
                        .map(SearchItem::new)
                        .toList()
                        .subscribe(view::displayData, mErrorHandler::handleError));
    }

    @Subscribe
    public void onInputTextChanged(SearchChannelTextChanged event) {
        super.onInputTextChanged(event);
    }

}
