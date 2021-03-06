package com.applikey.mattermost.mvp.presenters;

import com.applikey.mattermost.App;
import com.applikey.mattermost.events.SearchAllTextChanged;
import com.applikey.mattermost.events.SearchChannelTextChanged;
import com.applikey.mattermost.events.SearchMessageTextChanged;
import com.applikey.mattermost.events.SearchUserTextChanged;
import com.applikey.mattermost.mvp.views.SearchChatView;
import com.arellomobile.mvp.InjectViewState;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

@InjectViewState
public class SearchChatPresenter extends BasePresenter<SearchChatView> {

    @Inject
    EventBus mBus;

    public SearchChatPresenter() {
        App.getUserComponent().inject(this);
    }

    public void handleUserTextChanges(String text) {
        mBus.post(new SearchUserTextChanged(text));
    }

    public void handleChannelTextChanges(String text) {
        mBus.post(new SearchChannelTextChanged(text));
    }

    public void handleMessageTextChanges(String text) {
        mBus.post(new SearchMessageTextChanged(text));
    }

    public void handleAllTextChanges(String text) {
        mBus.post(new SearchAllTextChanged(text));
    }
}
