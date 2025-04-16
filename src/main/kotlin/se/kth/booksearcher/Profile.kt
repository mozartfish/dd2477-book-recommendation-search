package se.kth.booksearcher

import se.kth.booksearcher.data.UserProfile
import java.io.File

fun listProfiles() : List<String> {
    val dir = File("profiles")
    return dir.listFiles()?.map { it.nameWithoutExtension } ?: emptyList()
}

fun loadProfile(profileName: String) : UserProfile {
    val file = File("profiles/$profileName.profile")
    return if (!file.exists()) {
        File("profiles").mkdirs()
        file.createNewFile()
        UserProfile(profileName, emptySet())
    }else {
        UserProfile(profileName, file.readLines().toSet())
    }
}

fun saveProfile(profile: UserProfile) {
    val file = File("profiles/${profile.username}.profile")
    file.parentFile.mkdirs()
    file.writeText(profile.readBooks.joinToString("\n"))
}
