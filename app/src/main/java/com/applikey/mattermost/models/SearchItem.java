package com.applikey.mattermost.models;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface SearchItem {

    int USER = 0;

    int CHANNEL = 1;

    int MESSAGE = 2;

    int MESSAGE_CHANNEL = 3;

    int PRIORITY_USER = 0;

    int PRIORITY_MESSAGE = 1;

    int PRIORITY_CHANNEL = 2;

    @Retention(SOURCE)
    @IntDef({CHANNEL, USER, MESSAGE, MESSAGE_CHANNEL})
    @interface Type {

    }

    @Type
    int getSearchType();

    int getSortPriority();

    abstract int compareByDate(SearchItem item);
}
