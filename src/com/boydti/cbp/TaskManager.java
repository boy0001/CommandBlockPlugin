package com.boydti.cbp;

import java.util.HashMap;

import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Bukkit;

public class TaskManager
{
    public static int taskRepeat(final Runnable r, final int interval)
    {
        return Main.get().getServer().getScheduler().scheduleSyncRepeatingTask(Main.get(), r, interval, interval);
    }

    public static MutableInt index = new MutableInt(0);
    public static HashMap<Integer, Integer> tasks = new HashMap<>();

    public static void taskAsync(final Runnable r)
    {
        if (r == null) { return; }
        Bukkit.getServer().getScheduler().runTaskAsynchronously(Main.get(), r).getTaskId();
    }

    public static void task(final Runnable r)
    {
        if (r == null) { return; }
        Bukkit.getServer().getScheduler().runTask(Main.get(), r).getTaskId();
    }

    public static void taskLater(final Runnable r, final int delay)
    {
        if (r == null) { return; }
        Main.get().getServer().getScheduler().runTaskLater(Main.get(), r, delay).getTaskId();
    }

    public static void taskLaterAsync(final Runnable r, final int delay)
    {
        Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(Main.get(), r, delay);
    }

    public static void cancelTask(final int task)
    {
        if (task != -1)
        {
            Bukkit.getScheduler().cancelTask(task);
        }
    }

    public static int taskRepeatAsync(final Runnable r, final int interval)
    {
        return Main.get().getServer().getScheduler().scheduleAsyncRepeatingTask(Main.get(), r, interval, interval);
    }
}
