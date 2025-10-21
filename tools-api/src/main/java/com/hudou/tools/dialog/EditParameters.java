package com.hudou.tools.dialog;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class EditParameters implements Parcelable {
    private String hint;
    private boolean onlyNumbers;

    public EditParameters() {}

    protected EditParameters(Parcel in) {
        hint = in.readString();
        onlyNumbers = in.readByte() != 0;
    }

    public static final Creator<EditParameters> CREATOR = new Creator<>() {
        @Override
        public EditParameters createFromParcel(Parcel in) {
            return new EditParameters(in);
        }

        @Override
        public EditParameters[] newArray(int size) {
            return new EditParameters[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(hint);
        parcel.writeByte((byte) (onlyNumbers ? 1 : 0));
    }

    public static class Builder {
        private final EditParameters parameters;

        public Builder() {
            this.parameters = new EditParameters();
        }

        public Builder hint(String hint) {
            parameters.hint = hint;
            return this;
        }

        public Builder onlyNumbers(boolean onlyNumbers) {
            parameters.onlyNumbers = onlyNumbers;
            return this;
        }

        public EditParameters build() {
            return parameters;
        }
    }
}
