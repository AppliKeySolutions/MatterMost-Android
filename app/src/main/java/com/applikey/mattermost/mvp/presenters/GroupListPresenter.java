package com.applikey.mattermost.mvp.presenters;

import com.applikey.mattermost.mvp.views.ChatListView;
import com.applikey.mattermost.web.ErrorHandler;
import com.arellomobile.mvp.InjectViewState;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@InjectViewState
public class GroupListPresenter extends BaseChatListPresenter {

    public GroupListPresenter() {
        super();
    }

    @Override
    public void getInitialData() {
        final ChatListView view = getViewState();
        mSubscription.add(
                mChannelStorage.listClosed()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view::displayInitialData, ErrorHandler::handleError));
    }
}
