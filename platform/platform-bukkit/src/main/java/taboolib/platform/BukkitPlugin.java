package taboolib.platform;

import org.bukkit.Bukkit;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tabooproject.reflex.Reflex;
import taboolib.common.LifeCycle;
import taboolib.common.PrimitiveIO;
import taboolib.common.TabooLib;
import taboolib.common.classloader.IsolatedClassLoader;
import taboolib.common.platform.Platform;
import taboolib.common.platform.PlatformSide;
import taboolib.common.platform.Plugin;

import java.io.File;
import java.util.Set;

/**
 * TabooLib
 * taboolib.platform.BukkitPlugin
 *
 * @author sky
 * @since 2021/6/26 8:22 下午
 */
@SuppressWarnings({"DuplicatedCode", "CallToPrintStackTrace"})
@PlatformSide(Platform.BUKKIT)
public class BukkitPlugin extends JavaPlugin {

    @Nullable
    private static final Plugin pluginInstance;
    private static BukkitPlugin instance;

    static {
        // 初始化 IsolatedClassLoader
        long time = System.currentTimeMillis();
        try {
            IsolatedClassLoader.init(BukkitPlugin.class);
            // 排除两个接口
            IsolatedClassLoader.INSTANCE.addExcludedClass("taboolib.platform.BukkitWorldGenerator");
            IsolatedClassLoader.INSTANCE.addExcludedClass("taboolib.platform.BukkitBiomeProvider");
        } catch (Throwable ex) {
            TabooLib.setStopped(true);
            PrimitiveIO.error("Failed to initialize primitive loader, the plugin \"%s\" will be disabled!", PrimitiveIO.getRunningFileName());
            throw ex;
        }
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.CONST);
        // 检索 TabooLib Plugin 实现
        pluginInstance = Plugin.getImpl();
        // 调试模式显示加载耗时
        PrimitiveIO.debug("\"%s\" Initialization completed. (%sms)", PrimitiveIO.getRunningFileName(), System.currentTimeMillis() - time);
    }

    public BukkitPlugin() {
        instance = this;
        // 修改访问提示（似乎有用）
        injectIllegalAccess();
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.INIT);
    }

    @Override
    public void onLoad() {
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.LOAD);
        // 调用 Plugin 实现的 onLoad() 方法
        if (pluginInstance != null && !TabooLib.isStopped()) {
            pluginInstance.onLoad();
        }
    }

    @Override
    public void onEnable() {
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.ENABLE);
        // 判断插件是否关闭
        if (!TabooLib.isStopped()) {
            // 调用 Plugin 实现的 onEnable() 方法
            if (pluginInstance != null) {
                pluginInstance.onEnable();
            }
        }
        // 再次判断插件是否关闭
        // 因为插件可能在 onEnable() 下关闭
        if (!TabooLib.isStopped()) {
            // 创建调度器，执行 onActive() 方法
            if (Folia.isFolia) {
                FoliaExecutor.ASYNC_SCHEDULER.runNow(this, task -> invokeActive());
            } else {
                Bukkit.getScheduler().runTask(this, this::invokeActive);
            }
        }
    }

    @Override
    public void onDisable() {
        // 在插件未关闭的前提下，执行 onDisable() 方法
        if (pluginInstance != null && !TabooLib.isStopped()) {
            pluginInstance.onDisable();
        }
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.DISABLE);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        if (pluginInstance instanceof BukkitWorldGenerator) {
            return ((BukkitWorldGenerator) pluginInstance).getDefaultWorldGenerator(worldName, id);
        }
        return null;
    }

    @Nullable
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull String worldName, @Nullable String id) {
        if (pluginInstance instanceof BukkitBiomeProvider) {
            return ((BukkitBiomeProvider) pluginInstance).getDefaultBiomeProvider(worldName, id);
        }
        return null;
    }

    @NotNull
    @Override
    public File getFile() {
        return super.getFile();
    }

    @Nullable
    public static Plugin getPluginInstance() {
        return pluginInstance;
    }

    @NotNull
    public static BukkitPlugin getInstance() {
        return instance;
    }

    /**
     * 运行 onActive() 方法
     */
    private void invokeActive() {
        // 生命周期任务
        TabooLib.lifeCycle(LifeCycle.ACTIVE);
        // 调用 Plugin 实现的 onActive() 方法
        if (pluginInstance != null) {
            pluginInstance.onActive();
        }
    }

    /**
     * 移除 Spigot 的访问警告：
     * Loaded class {0} from {1} which is not a depend, softdepend or loadbefore of this plugin
     */
    @SuppressWarnings("DataFlowIssue")
    public static void injectIllegalAccess() {
        try {
            long time = System.currentTimeMillis();
            PluginDescriptionFile description = Reflex.Companion.getLocalProperty(BukkitPlugin.class.getClassLoader(), "description");
            Set<String> accessSelf = Reflex.Companion.getLocalProperty(BukkitPlugin.class.getClassLoader(), "seenIllegalAccess");
            for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                if (plugin.getClass().getName().endsWith("platform.BukkitPlugin")) {
                    Set<String> accessOther = Reflex.Companion.getLocalProperty(plugin.getClass().getClassLoader(), "seenIllegalAccess");
                    accessOther.add(description.getName());
                    accessSelf.add(plugin.getName());
                }
            }
            PrimitiveIO.debug("Injected illegal access warning. (%sms)", System.currentTimeMillis() - time);
        } catch (Throwable ignored) {
        }
    }
}
