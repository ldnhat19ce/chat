package vku.ltnhan.friendchat.models

class ChatMessage(
        val id: String,
        val text: String,
        val fromId: String,
        val toId: String,
        val timestamp: Long,
        val type : Int
) {
    constructor() : this("", "", "", "", -1, -1)
}