package net.creeperhost.minetogether.data;

import net.creeperhost.minetogether.CreeperHost;
import net.creeperhost.minetogether.chat.ChatHandler;
import net.creeperhost.minetogether.chat.Message;
import net.creeperhost.minetogether.common.LimitedSizeQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class KnownUsers
{
    private AtomicReference<List<Profile>> profiles = new AtomicReference<List<Profile>>();

    public KnownUsers()
    {
        this.profiles.set(new ArrayList<Profile>());
    }

    public void clean()
    {
        List<String> remove = new ArrayList<>();
        for(Profile profile : profiles.get())
        {
            if(profile.getUserDisplay().isEmpty()) continue;
            if(profile.isFriend()) continue;
            if(profile.isBanned()) continue;
            //Check if chatlines contains profile.getDisplay();
            LimitedSizeQueue<Message> tempMessages = ChatHandler.messages.get(ChatHandler.CHANNEL);
            boolean skip = false;
            for(Message message : tempMessages)
            {
                if(message.sender.equals("System")) continue;
                if(message.sender.equalsIgnoreCase(profile.getUserDisplay()) || message.sender.equalsIgnoreCase(profile.getMediumHash()) || message.sender.equalsIgnoreCase(profile.getShortHash()))
                {
                    skip = true;
                    break;
                }
            }
            if(!skip)
            {
                //Cleanup legacy code curseSync list...
                if(ChatHandler.curseSync.containsKey(profile.getMediumHash()))
                {
                    ChatHandler.curseSync.remove(profile.getMediumHash());
                } else if(ChatHandler.curseSync.containsKey(profile.getShortHash()))
                {
                    ChatHandler.curseSync.remove(profile.getShortHash());
                }
                remove.add(profile.getLongHash());
            }
        }
        for(String hash : remove)
        {
            removeByHash(hash, false);
        }
    }

    public Profile add(String hash)
    {
        if(CreeperHost.profile.get().getLongHash().startsWith(hash.substring(2))) return null;

        Profile profile = new Profile(hash);
        if(findByNick(hash) == null)
        {
            profiles.updateAndGet(profiles1 ->
            {
                profiles1.add(profile);
//                CreeperHost.logger.warn("Adding " + hash + " to knownusers " + profiles.get().size());
                return profiles1;
            });
            CompletableFuture.runAsync(() -> {
                Profile profileFuture = findByNick(hash);
                profileFuture.loadProfile();
            }, CreeperHost.instance.profileExecutor);
            return profile;
        }
        return null;
    }
    public boolean update(Profile updatedProfile)
    {
        //No update without it being a completed profile
        if(updatedProfile.longHash.length() == 0) return false;
        profiles.updateAndGet((curProfiles) -> {
            try {
                Profile existingProfile = findByHash(updatedProfile.longHash);
                curProfiles.remove(existingProfile);
            } catch(Exception ignored) {}
            curProfiles.add(updatedProfile);
            return curProfiles;
        });
        return (findByHash(updatedProfile.longHash) == updatedProfile);
    }

    public void removeByHash(String hash, boolean ignoreFriend)
    {
        profiles.updateAndGet(profiles1 ->
        {
            Profile profileTarget = findByHash(hash);
            if(profileTarget == null) return profiles1;
            if(profileTarget.isBanned()) return profiles1;
            if(ignoreFriend) {
                if (profileTarget.isFriend()) return profiles1;
            }
            profiles1.remove(profileTarget);
            return profiles1;
        });
    }
    public void removeByNick(String nick, boolean ignoreFriend)
    {
        profiles.updateAndGet(profiles1 ->
        {
            Profile profileTarget = findByNick(nick);
            if(profileTarget == null) return profiles1;
            if(profileTarget.isBanned()) return profiles1;
            if(ignoreFriend) {
                if (profileTarget.isFriend()) return profiles1;
            }
            profiles1.remove(profileTarget);
            return profiles1;
        });
    }

    public Profile findByHash(String search)
    {
        List<Profile> profilesCopy = new ArrayList<Profile>(profiles.get());
        Profile returnProfile = null;
        for(Profile profile : profilesCopy) {
            if (profile.getLongHash().equalsIgnoreCase(search))
            {
                returnProfile = profile;
                break;
            }
        }
        return returnProfile;
    }

    public Profile findByDisplay(String search)
    {
        List<Profile> profilesCopy = new ArrayList<Profile>(profiles.get());
        Profile returnProfile = null;
        for(Profile profile : profilesCopy)
        {
            if(profile.getUserDisplay().equalsIgnoreCase(search))
            {
                returnProfile = profile;
                break;
            }
        }
        return returnProfile;
    }

    public Profile findByNick(String search)
    {
        List<Profile> profilesCopy = new ArrayList<Profile>(profiles.get());
        Profile returnProfile = null;
        for(Profile profile : profilesCopy)
        {
            if(profile.getShortHash().equalsIgnoreCase(search) || profile.getMediumHash().equalsIgnoreCase(search))
            {
                returnProfile = profile;
                break;
            }
        }
        return returnProfile;
    }
}
