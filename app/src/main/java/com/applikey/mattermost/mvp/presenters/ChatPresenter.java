package com.applikey.mattermost.mvp.presenters;

import android.text.TextUtils;

import com.applikey.mattermost.App;
import com.applikey.mattermost.manager.notitifcation.NotificationManager;
import com.applikey.mattermost.models.channel.Channel;
import com.applikey.mattermost.models.post.PendingPost;
import com.applikey.mattermost.models.post.Post;
import com.applikey.mattermost.mvp.views.ChatView;
import com.applikey.mattermost.storage.db.ChannelStorage;
import com.applikey.mattermost.storage.db.PostStorage;
import com.applikey.mattermost.storage.db.UserStorage;
import com.applikey.mattermost.storage.preferences.Prefs;
import com.applikey.mattermost.web.Api;
import com.applikey.mattermost.web.ErrorHandler;
import com.arellomobile.mvp.InjectViewState;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@InjectViewState
public class ChatPresenter extends BasePresenter<ChatView> {

    private static final int PAGE_SIZE = 60;
    private static final String CHANNEL_PREFIX = "#";
    private static final String DIRECT_PREFIX = "";

    @Inject
    PostStorage mPostStorage;

    @Inject
    UserStorage mUserStorage;

    @Inject
    ChannelStorage mChannelStorage;

    @Inject
    Api mApi;

    @Inject
    Prefs mPrefs;

    @Inject
    NotificationManager mNotificationManager;

    @Inject
    ErrorHandler mErrorHandler;

    private Channel mChannel;
    private String mTeamId;

    private boolean mFirstFetched = false;

    public ChatPresenter() {
        App.getUserComponent().inject(this);
    }

    public void getInitialData(String channelId) {
        final ChatView view = getViewState();

        updateLastViewedAt(channelId);

        mTeamId = mPrefs.getCurrentTeamId();

        mSubscription.add(mChannelStorage.channelById(channelId)
                                  .distinctUntilChanged()
                                  .doOnNext(channel -> mChannel = channel)
                                  .doOnNext(channel -> fetchFirstPageWithClear())
                                  .map(channel -> {
                                      final String prefix = !mChannel.getType().equals(Channel.ChannelType.DIRECT.getRepresentation())
                                              ? CHANNEL_PREFIX : DIRECT_PREFIX;
                                      return prefix + channel.getDisplayName();
                                  })
                                  .subscribe(view::showTitle, mErrorHandler::handleError));

        mSubscription.add(mPostStorage.listByChannel(channelId)
                                  .first()
                                  .doOnNext(posts -> getViewState().showEmpty(posts.isEmpty()))
                                  .subscribe(view::onDataReady, mErrorHandler::handleError));
    }

    public void channelNameClick() {
        final ChatView view = getViewState();
        if (Channel.ChannelType.DIRECT.getRepresentation().equals(mChannel.getType())) {
            view.openUserProfile(mChannel.getDirectCollocutor());
        } else {
            view.openChannelDetails(mChannel);
        }
    }

    public void fetchAfterRestart() {
        if (!mFirstFetched) {
            return;
        }
        fetchPage(0, false);
    }

    public void fetchFirstPageWithClear() {
        fetchPage(0, true);
    }

    public void fetchNextPage(int totalItems) {
        fetchPage(totalItems, false);
    }

    public void deleteMessage(String channelId, Post post) {
        mSubscription.add(mApi.deletePost(mTeamId, channelId, post.getId())
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .doOnNext(posts -> mPostStorage.delete(post))
                                  .doOnNext(posts -> mChannel.setLastPost(null))
                                  .subscribe(posts -> mChannelStorage.updateLastPost(mChannel), mErrorHandler::handleError));
    }

    public void editMessage(String channelId, Post post, String newMessage) {
        final Post finalPost = mPostStorage.copyFromDb(post);
        finalPost.setMessage(newMessage);
        mSubscription.add(mApi.updatePost(mTeamId, channelId, finalPost)
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(posts -> mPostStorage.update(finalPost), mErrorHandler::handleError));
    }

    public void sendMessage(String channelId, String message) {
        if (TextUtils.isEmpty(message.trim())) {
            return;
        }
        final String currentUserId = mPrefs.getCurrentUserId();
        final long createdAt = System.currentTimeMillis();
        final String pendingId = currentUserId + ":" + createdAt;

        final PendingPost pendingPost = new PendingPost(createdAt, currentUserId, channelId,
                                                        message, "", pendingId);

        mSubscription.add(mApi.createPost(mTeamId, channelId, pendingPost)
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .doOnNext(post -> mChannelStorage.setLastViewedAt(channelId, post.getCreatedAt()))
                                  .doOnNext(post -> mChannelStorage.setLastPost(mChannel, post))
                                  .subscribe(result -> getViewState().onMessageSent(result.getCreatedAt()), mErrorHandler::handleError));
    }

    public void sendReplyMessage(String channelId, String message, String mRootId) {
        final String currentUserId = mPrefs.getCurrentUserId();
        final long createdAt = System.currentTimeMillis();
        final String pendingId = currentUserId + ":" + createdAt;

        final PendingPost pendingPost = new PendingPost(createdAt, currentUserId, channelId,
                                                        message, "", pendingId, mRootId, mRootId);

        mSubscription.add(mApi.createPost(mTeamId, channelId, pendingPost)
                                  .subscribeOn(Schedulers.io())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .doOnNext(post -> mChannelStorage.setLastViewedAt(channelId, post.getCreatedAt()))
                                  .doOnNext(post -> mChannelStorage.setLastPost(mChannel, post))
                                  .subscribe(result -> getViewState().onMessageSent(result.getCreatedAt()),
                                             mErrorHandler::handleError
                                            ));
    }

    private void updateLastViewedAt(String channelId) {
        // Does not belong to UI
        mApi.updateLastViewedAt(mTeamId, channelId)
                .subscribeOn(Schedulers.io())
                .toCompletable()
                .subscribe(() -> {
                }, mErrorHandler::handleError);

        mChannelStorage.updateLastViewedAt(channelId);
        mNotificationManager.dismissNotification(channelId);
    }

    private void fetchPage(int totalItems, boolean clear) {
        getViewState().showProgress(true);
        final String channelId = mChannel.getId();
        mSubscription.add(mApi.getPostsPage(mTeamId, channelId, totalItems, PAGE_SIZE)
                                  .subscribeOn(Schedulers.io())
                                  .switchIfEmpty(Observable.empty())
                                  .map(postResponse -> new ArrayList<>(postResponse.getPosts().values()))
                                  .doOnNext(posts -> posts.sort(Post::COMPARATOR_BY_CREATE_AT))
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(posts -> {
                                      getViewState().showProgress(false);
                                      if (clear) {
                                          clearChat();
                                      }
                                      if (totalItems == 0 && posts.size() > 0) {
                                          mChannelStorage.setLastPost(mChannel, posts.get(0));
                                      }
                                      mFirstFetched = true;
                                      mPostStorage.saveAll(posts);
                                  }, error -> {
                                      getViewState().showProgress(false);
                                      mErrorHandler.handleError(error);
                                  }));
    }

    private void clearChat() {
        mPostStorage.deleteAllByChannel(mChannel.getId());
    }
}
