package com.ravenpos.ravendspreadpos.utils.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Keep;

import com.ravenpos.ravendspreadpos.BaseApplication;

import java.util.Arrays;

@Keep
public class SharedPreferencesUtils {
    public static final String KEYS = "KEYS";
    private static SharedPreferencesUtils mSharedPreferencesUtils;
    protected Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPreferencesEditor;
    private static final String PREF_NAME = "fcmb_pref";

    private SharedPreferencesUtils(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }

    public static synchronized SharedPreferencesUtils getInstance() {
        if (mSharedPreferencesUtils == null) {
            mSharedPreferencesUtils = new SharedPreferencesUtils(BaseApplication.getINSTANCE());
        }
        return mSharedPreferencesUtils;
    }

    /**
     * Stores String value in preferenes
     *
     * @param key   key for preference
     * @param value value for that key
     */
    public void setValue(String key, String value) {
        mSharedPreferencesEditor.putString(key, value);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Stores int value in preferenes
     *
     * @param key   key for preference
     * @param value value for that key
     */
    public void setValue(String key, int value) {
        mSharedPreferencesEditor.putInt(key, value);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Stores double value in preferenes
     *
     * @param key   key for preference
     * @param value value for that key
     */
    public void setValue(String key, double value) {
        setValue(key, String.valueOf(value));
    }

    /**
     * Stores long value in preferenes
     *
     * @param key   key for preference
     * @param value value for that key
     */
    public void setValue(String key, long value) {
        mSharedPreferencesEditor.putLong(key, value);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Stores byte array value in preferene,
     * this will be used to save our encryption
     * public and private keys in shared preferences
     *
     * @param key   key for preference
     * @param value value for the key
     */
    public void setValue(String key, byte[] value) {
        setValue(key, Arrays.toString(value));
    }

    /**
     * Stores boolean value in preferenes
     *
     * @param key   key for preference
     * @param value value for that key
     */
    public void setValue(String key, boolean value) {
        mSharedPreferencesEditor.putBoolean(key, value);
        mSharedPreferencesEditor.apply();
    }

    /**
     * Retreives String value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if key not found in the preferene
     * @return String value associated with the key
     */
    public String getStringValue(String key, String defaultValue) {
        return mSharedPreferences.getString(key, defaultValue);
    }

    /**
     * Retreives int value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if key not found in the preferene
     * @return int value associated with the key
     */
    public int getIntValue(String key, int defaultValue) {
        return mSharedPreferences.getInt(key, defaultValue);
    }

    /**
     * Retreives long value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if key not found in the preferene
     * @return long value associated with the key
     */
    public long getLongValue(String key, long defaultValue) {
        return mSharedPreferences.getLong(key, defaultValue);
    }

    /**
     * Retreives boolean value from preference
     *
     * @param key          key of preference
     * @param defaultValue default value if key not found in the preferene
     * @return boolean value associated with the key
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return mSharedPreferences.getBoolean(key, defaultValue);
    }

    public byte[] getByteArrayValue(String key) {
        byte[] array = null;
        String StringArray = mSharedPreferences.getString(key, "");
        if (StringArray != null) {
            String[] split = StringArray.substring(1, StringArray.length() - 1)
                    .split(", ");
            array = new byte[split.length];
            for (int i = 0; i < split.length; i++) {
                array[i] = Byte.parseByte(split[i]);
            }
        }
        return array;
    }

    /**
     * Removes key from preference
     *
     * @param key key of preference that is to be removed
     */
    public void removesKey(String key) {
        if (mSharedPreferencesEditor != null) {
            mSharedPreferencesEditor.remove(key);
            mSharedPreferencesEditor.apply();
        }
    }

    /**
     * Clear all the preferences stored
     */
    public void clear() {
        mSharedPreferencesEditor.clear().apply();

    }
}