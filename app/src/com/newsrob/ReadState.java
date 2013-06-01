package com.newsrob;

public enum ReadState {
    READ, UNREAD, PINNED;

    public static ReadState fromInt(int readState) {
        switch (readState) {
        case 0:
            return ReadState.UNREAD;
        case 1:
            return ReadState.READ;
        default:
            return ReadState.PINNED;
        }
    }

    public static int toInt(ReadState readState) {
        switch (readState) {
        case UNREAD:
            return 0;
        case READ:
            return 1;
        default:
            return -1;
        }
    }
}
