package com.applikey.mattermost.mvp.presenters;

import com.applikey.mattermost.App;
import com.applikey.mattermost.models.channel.ChannelResponse;
import com.applikey.mattermost.models.team.Team;
import com.applikey.mattermost.models.user.User;
import com.applikey.mattermost.models.web.StartupFetchResult;
import com.applikey.mattermost.mvp.views.ChooseTeamView;
import com.applikey.mattermost.storage.db.ChannelStorage;
import com.applikey.mattermost.storage.db.TeamStorage;
import com.applikey.mattermost.storage.db.UserStorage;
import com.applikey.mattermost.storage.preferences.Prefs;
import com.applikey.mattermost.utils.image.ImagePathHelper;
import com.applikey.mattermost.web.Api;
import com.applikey.mattermost.web.ErrorHandler;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;

public class ChooseTeamPresenter extends SingleViewPresenter<ChooseTeamView> {

    @Inject
    Api mApi;

    @Inject
    Prefs mPrefs;

    @Inject
    TeamStorage mTeamStorage;

    @Inject
    UserStorage mUserStorage;

    @Inject
    ChannelStorage mChannelStorage;

    @Inject
    ImagePathHelper mImagePathHelper;

    public ChooseTeamPresenter() {
        App.getComponent().inject(this);
    }

    public void getInitialData() {
        final ChooseTeamView view = getView();
        mSubscription.add(mTeamStorage.listAll()
                .subscribe(view::displayTeams, throwable -> {
                    ErrorHandler.handleError(throwable);
                    getView().onFailure(throwable.getMessage());
                }));
    }

    public void chooseTeam(Team team) {
        mTeamStorage.setChosenTeam(team);
        final ChooseTeamView view = getView();

        // Perform Startup Fetch
        mSubscription.add(Observable.zip(
                mApi.listChannels(team.getId()),
                mApi.getTeamProfiles(team.getId()),
                this::transform)
                .subscribeOn(Schedulers.io())
                .doOnNext(response -> {
                    final ChannelResponse channelResponse = response.getChannelResponse();
                    mChannelStorage.saveChannelResponse(channelResponse);
                    final Map<String, User> contacts = response.getDirectProfiles();
                    addImagePathInfo(contacts);
                    mUserStorage.saveUsers(contacts);

                    final Set<String> keys = contacts.keySet();
                    final String[] userIds = keys.toArray(new String[]{});

                    // TODO: Remove v3.3 API support

                    // v3.3 Compatible
                    mSubscription.add(mApi.getUserStatusesCompatible(userIds)
                            .subscribeOn(Schedulers.io())
                            .doOnNext(response1 -> {
                                mUserStorage.saveStatuses(response1);
                            })
                            .observeOn(Schedulers.io())
                            .subscribe(response2 -> {
                                view.onTeamChosen();
                            }, error -> {
                                // v3.4 Compatible
                                // we are trying to call modern API
                                mSubscription.add(mApi.getUserStatuses()
                                        .subscribeOn(Schedulers.io())
                                        .doOnNext(response1 -> {
                                            mUserStorage.saveStatuses(response1);
                                        })
                                        .observeOn(Schedulers.io())
                                        .subscribe(response2 -> {
                                            view.onTeamChosen();
                                        }, ErrorHandler::handleError));
                            }));
                })
                .observeOn(Schedulers.io()) // Try main thread
                .subscribe(response -> {
                }, ErrorHandler::handleError));
    }

    private void addImagePathInfo(Map<String, User> users) {
        for (User user : users.values()) {
            user.setProfileImage(mImagePathHelper.getProfilePicPath(user.getId()));
        }
    }

    private StartupFetchResult transform(ChannelResponse channelResponse,
                                         Map<String, User> directProfiles) {
        return new StartupFetchResult(channelResponse, directProfiles);
    }
}
