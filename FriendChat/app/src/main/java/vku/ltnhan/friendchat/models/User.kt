package vku.ltnhan.friendchat.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(
        val uid: String,
        val username: String,
        val profileImageUrl: String,
        val token_key:String,
        val profileCoverUrl:String
): Parcelable
{
    constructor() : this("", "", "", "", "")
}