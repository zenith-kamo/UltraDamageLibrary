package com.zenith.udl.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class MixinProtectionPlugin implements IMixinConfigPlugin {

    // 1. クラスがJVMにロードされた瞬間に実行（最速発動）
    static {
        protectAll();
    }

    // 2. プラグインインスタンス化時に実行
    public MixinProtectionPlugin() {
        protectAll();
    }

    // 3. 通常の初期化イベント時に実行
    @Override
    public void onLoad(String mixinPackage) {
        protectAll();
    }

    /**
     * pendingConfigs および、その内部の全 ClassNode.methods を保護リストに置換する (static化)
     */
    public static void protectAll() {
        try {
            MixinEnvironment env = MixinEnvironment.getCurrentEnvironment();

            Field transformerField = env.getClass().getDeclaredField("transformer");
            transformerField.setAccessible(true);
            Object transformer = transformerField.get(env);

            Field processorField = transformer.getClass().getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(transformer);

            Field pendingConfigsField = processor.getClass().getDeclaredField("pendingConfigs");
            pendingConfigsField.setAccessible(true);

            Object currentConfigs = pendingConfigsField.get(processor);

            if (currentConfigs instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> configsList = (List<Object>) currentConfigs;

                // 1. pendingConfigs 自体を保護 (main_ver10 対策)
                if (!(configsList instanceof ProtectedList)) {
                    ProtectedList<Object> protectedConfigs = new ProtectedList<>(configsList);
                    pendingConfigsField.set(processor, protectedConfigs);
                    configsList = protectedConfigs;
                    System.out.println("[ProtectionPlugin] pendingConfigs を保護しました。");
                }

                // 2. pendingConfigs 内部の classNode.methods もすべて保護 (main_ver20 対策)
                protectClassNodeMethods(configsList);
            }
        } catch (Exception e) {
            System.err.println("[ProtectionPlugin] 保護処理に失敗しました:");
            e.printStackTrace();
        }
    }

    /**
     * 他Modがリフレクションで取得して改変しようとする ClassNode.methods を保護する (static化)
     */
    private static void protectClassNodeMethods(List<Object> configs) {
        for (Object config : configs) {
            protectSingleConfig(config);
        }
    }

    /**
     * 単一の MixinConfig 内の ClassNode.methods を保護する
     */
    private static void protectSingleConfig(Object config) {
        if (config == null)
            return;
        try {
            Field mixinsField = config.getClass().getDeclaredField("mixins");
            mixinsField.setAccessible(true);
            List<?> mixinInfos = (List<?>) mixinsField.get(config);

            if (mixinInfos == null)
                return;

            for (Object mixinInfo : mixinInfos) {
                try {
                    Field pendingStateField = mixinInfo.getClass().getDeclaredField("pendingState");
                    pendingStateField.setAccessible(true);
                    Object pendingState = pendingStateField.get(mixinInfo);

                    if (pendingState == null)
                        continue;

                    Field classNodeField = pendingState.getClass().getDeclaredField("classNode");
                    classNodeField.setAccessible(true);
                    ClassNode classNode = (ClassNode) classNodeField.get(pendingState);

                    if (classNode != null && classNode.methods != null) {
                        if (!(classNode.methods instanceof ProtectedList)) {
                            classNode.methods = new ProtectedList<>(classNode.methods);
                            System.out
                                    .println("[ProtectionPlugin] ClassNode.methods (" + classNode.name + ") を保護しました。");
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    /**
     * 未認証クラスからの変更・削除要求をブロックするカスタム List
     */
    public static class ProtectedList<E> extends ArrayList<E> {

        private static final ThreadLocal<Boolean> LOCK = ThreadLocal.withInitial(() -> false);

        private static final String[] ALLOWED_PREFIXES = {
                "com.zenith.",
                "org.spongepowered.",
                "net.minecraft.",
                "net.minecraftforge.",
                "cpw.mods.modlauncher."
        };

        public ProtectedList(Collection<? extends E> c) {
            super(c);
        }

        // --- 削除系操作のブロック ---

        @Override
        public boolean remove(Object o) {
            if (isAllowedCaller())
                return super.remove(o);
            System.out.println("[ProtectionPlugin] 外部Modからの remove(Object) をブロックしました。");
            return false;
        }

        @Override
        public E remove(int index) {
            if (isAllowedCaller())
                return super.remove(index);
            System.out.println("[ProtectionPlugin] 外部Modからの remove(int) をブロックしました。");
            return null;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return isAllowedCaller() && super.removeAll(c);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return isAllowedCaller() && super.removeIf(filter);
        }

        @Override
        public void clear() {
            if (isAllowedCaller())
                super.clear();
            else
                System.out.println("[ProtectionPlugin] 外部Modからの clear() をブロックしました。");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return isAllowedCaller() && super.retainAll(c);
        }

        // --- 改変・置換操作のブロック ---

        @Override
        public E set(int index, E element) {
            if (isAllowedCaller())
                return super.set(index, element);
            System.out.println("[ProtectionPlugin] 外部Modからの set(int, E) による置換をブロックしました。");
            return get(index);
        }

        // --- 追加操作（後から追加された遅延MODの自動保護） ---

        @Override
        public boolean add(E element) {
            protectSingleConfig(element);
            return super.add(element);
        }

        @Override
        public void add(int index, E element) {
            protectSingleConfig(element);
            super.add(index, element);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            for (E element : c) {
                protectSingleConfig(element);
            }
            return super.addAll(c);
        }

        // --- 呼び出し元検証 ---

        private boolean isAllowedCaller() {
            if (LOCK.get())
                return true;
            LOCK.set(true);
            try {
                return StackWalker.getInstance()
                        .walk(frames -> frames
                                .map(StackWalker.StackFrame::getClassName)
                                .filter(name -> !name.startsWith("java.") &&
                                        !name.startsWith("jdk.") &&
                                        !name.startsWith("sun.") &&
                                        !name.contains("ProtectedList"))
                                .findFirst()
                                .map(caller -> {
                                    for (String prefix : ALLOWED_PREFIXES) {
                                        if (caller.startsWith(prefix)) {
                                            return true;
                                        }
                                    }
                                    System.out.println("[ProtectionPlugin] 拒否された呼び出し元: " + caller);
                                    return false;
                                })
                                .orElse(false));
            } catch (Exception e) {
                return true;
            } finally {
                LOCK.set(false);
            }
        }
    }
}