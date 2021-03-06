package com.applikey.mattermost.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.applikey.mattermost.Constants;
import com.applikey.mattermost.R;
import com.applikey.mattermost.models.channel.Channel;
import com.applikey.mattermost.models.user.User;
import com.applikey.mattermost.mvp.presenters.BaseEditChannelPresenter;
import com.applikey.mattermost.mvp.presenters.EditChannelPresenter;
import com.applikey.mattermost.mvp.views.EditChannelView;
import com.applikey.mattermost.views.AddedPeopleLayout;
import com.arellomobile.mvp.presenter.InjectPresenter;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class EditChannelActivity extends BaseEditChannelActivity implements EditChannelView {

    private static final String CHANNEL_ID_KEY = "channel-id";
    private static final String INVITE_KEY = Constants.PACKAGE_NAME + "activities.editchannelactivity.invite";

    @BindView(R.id.add_members_text)
    TextView mAddMembersText;

    @BindView(R.id.members_layout)
    AddedPeopleLayout mMembersLayout;

    @InjectPresenter
    EditChannelPresenter mPresenter;
    @BindView(R.id.scroll_view) NestedScrollView mScrollView;
    @BindView(R.id.et_link_name) EditText mEtLinkName;
    @BindView(R.id.members_text) TextView mMembersText;

    private boolean mOpenAsInviteNewMember;

    public static Intent getIntent(Context context, Channel channel, boolean invite) {
        final Intent intent = new Intent(context, EditChannelActivity.class);
        intent.putExtra(CHANNEL_ID_KEY, channel.getId());
        intent.putExtra(INVITE_KEY, invite);
        return intent;
    }

    public static Intent getIntent(Context context, Channel channel) {
        return getIntent(context, channel, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParameters();
        mMembersLayout.setImageLoader(mImageLoader);
    }

    public void scroolToAddMembers() {
        mScrollView.post(() -> mScrollView.scrollTo(0, (int) mAddMembersText.getY()));
    }

    @Override
    public void showChannelData(Channel channel) {
        mEtChannelName.setText(channel.getDisplayName());
        mEtLinkName.setText(channel.getName());
        mEtChannelDescription.setText(channel.getPurpose());
        final boolean isPrivate = channel.getType()
                .equals(Channel.ChannelType.PRIVATE.getRepresentation());
        @StringRes final int title = isPrivate
                ? R.string.edit_private_group
                : R.string.edit_public_channel;
        setTitle(getResources().getString(title));
    }

    @OnClick(R.id.btn_delete_channel)
    public void onDeleteChannelClick() {
        showConfirmationDialog();
    }

    private void showConfirmationDialog() {
        Dialog confirmationDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete_channel)
                .setMessage(R.string.delete_channel_confirm_dialog_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    showLoadingDialog();
                    mPresenter.deleteChannel();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        confirmationDialog.show();
    }

    @OnClick(R.id.members_layout)
    void onMembersPanelClick() {
        hideLoadingDialog();
        startActivity(AddedMembersActivity.getIntent(this, mMembersLayout.getUsers(), false));
    }

    @Override
    protected BaseEditChannelPresenter getPresenter() {
        return mPresenter;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_edit_group_or_channel;
    }

    @Override
    public void onChannelUpdated() {
        hideLoadingDialog();
        finish();
    }

    @Override
    public void onChannelDeleted() {
        hideLoadingDialog();
        final Intent intent = new Intent(this, ChatListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_create_channel) {
            updateChannel();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showMembers(List<User> users) {
        mAdapter.setAlreadyMemberUsers(users);
        mMembersLayout.showUsers(users);
        if (mOpenAsInviteNewMember) {
            scroolToAddMembers();
        }
    }

    private void initParameters() {
        final Bundle extras = getIntent().getExtras();
        final String channelId = extras.getString(CHANNEL_ID_KEY);
        mOpenAsInviteNewMember = getIntent().getBooleanExtra(INVITE_KEY, false);
        mPresenter.getInitialData(channelId);
    }

    private void updateChannel() {
        showLoadingDialog();
        final String channelName = mEtChannelName.getText().toString().trim();
        final String channelDescription = mEtChannelDescription.getText().toString().trim();
        final String channelLink = mEtLinkName.getText().toString().trim();
        mPresenter.updateChannel(channelName, channelDescription, channelLink);
    }
}
