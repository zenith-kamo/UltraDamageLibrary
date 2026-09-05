package com.zenith.udl.manager;

import com.zenith.udl.Udl;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.EventBus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UDLProtector {

    private final EntityBanManager banManager = new EntityBanManager();
    private final Object anonymousListener;

    private static volatile boolean running = true;

    private Field listenersField;

    public UDLProtector() {
        Udl.LOGGER.info("[UDLProtector] ===== Initializing UDLProtector =====");

        // Reflectionのフィールド取得をあらかじめ1回だけ行っておく
        try {
            Udl.LOGGER.info(
                    "[UDLProtector] Attempting to get EventBus#listeners field..."
            );
            listenersField = EventBus.class.getDeclaredField("listeners");
            listenersField.setAccessible(true);

            Udl.LOGGER.info(
                    "[UDLProtector] SUCCESS: EventBus#listeners field acquired. field={}",
                    listenersField
            );

        } catch (NoSuchFieldException e) {
            Udl.LOGGER.error(
                    "[UDLProtector] FAILED: Could not find EventBus#listeners field!",
                    e
            );
        } catch (Exception e) {
            Udl.LOGGER.error(
                    "[UDLProtector] FAILED: Unexpected exception while acquiring listeners field.",
                    e
            );
        }

        // Pig2のクラス名判定を回避するための匿名リスナー
        this.anonymousListener = new Object() {

            @net.minecraftforge.eventbus.api.SubscribeEvent
            public void onEntityJoin(
                    net.minecraftforge.event.entity.EntityJoinLevelEvent event
            ) {
                Udl.LOGGER.debug(
                        "[UDLProtector] EntityJoinLevelEvent received. entity={}, level={}",
                        event.getEntity(),
                        event.getLevel()
                );

                banManager.onEntityJoinLevel(event);
            }
        };

        Udl.LOGGER.info(
                "[UDLProtector] Anonymous listener created. listenerClass={}, identityHash={}",
                anonymousListener.getClass().getName(),
                System.identityHashCode(anonymousListener)
        );

        Udl.LOGGER.info("[UDLProtector] ===== Initialization complete =====");
    }

    public void startProtection() {
        Udl.LOGGER.info("[UDLProtector] ===== Starting protection =====");

        Udl.LOGGER.info("[UDLProtector] Step 1/3: registerSafely()");
        registerSafely();

        Udl.LOGGER.info("[UDLProtector] Step 2/3: applyMapShield()");
        applyMapShield();

        Udl.LOGGER.info("[UDLProtector] Step 3/3: startCounterThread()");
        startCounterThread();

        Udl.LOGGER.info("[UDLProtector] ===== Protection started =====");
    }

    // 登録状態を確認してから登録
    private void registerSafely() {
//        Udl.LOGGER.debug("[UDLProtector] registerSafely() called.");

        try {
            boolean registered = isAlreadyRegistered(anonymousListener);

//            Udl.LOGGER.debug(
//                    "[UDLProtector] Registration check result: registered={}",
//                    registered
//            );

            if (!registered) {
                Udl.LOGGER.info(
                        "[UDLProtector] Listener is NOT registered. Registering listener..."
                );

                MinecraftForge.EVENT_BUS.register(anonymousListener);

                Udl.LOGGER.info(
                        "[UDLProtector] SUCCESS: Listener registered. listenerClass={}",
                        anonymousListener.getClass().getName()
                );
            } else {
//                Udl.LOGGER.debug(
//                        "[UDLProtector] Listener is already registered. No action required."
//                );
            }

        } catch (Exception e) {
            Udl.LOGGER.error(
                    "[UDLProtector] ERROR in registerSafely()",
                    e
            );
        }
    }

    private boolean isAlreadyRegistered(Object listener) {
//        Udl.LOGGER.debug(
//                "[UDLProtector] Checking listener registration. listener={}",
//                listener
//        );

        if (listenersField == null) {
            Udl.LOGGER.warn(
                    "[UDLProtector] listenersField is NULL. Cannot check registration state."
            );
            return false;
        }

        try {
            Object fieldValue = listenersField.get(MinecraftForge.EVENT_BUS);
//
//            Udl.LOGGER.debug(
//                    "[UDLProtector] EventBus#listeners value obtained. type={}, value={}",
//                    fieldValue != null ? fieldValue.getClass().getName() : "null",
//                    fieldValue
//            );

            if (!(fieldValue instanceof ConcurrentHashMap)) {
                Udl.LOGGER.warn(
                        "[UDLProtector] EventBus#listeners is not a ConcurrentHashMap! actualType={}",
                        fieldValue != null
                                ? fieldValue.getClass().getName()
                                : "null"
                );

                return false;
            }

            ConcurrentHashMap<?, ?> listeners =
                    (ConcurrentHashMap<?, ?>) fieldValue;

            boolean contains = listeners.containsKey(listener);

//            Udl.LOGGER.debug(
//                    "[UDLProtector] listeners.containsKey(listener)={}. mapSize={}",
//                    contains,
//                    listeners.size()
//            );

            return contains;

        } catch (Exception e) {
            Udl.LOGGER.error(
                    "[UDLProtector] ERROR while checking listener registration.",
                    e
            );

            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMapShield() {
//        Udl.LOGGER.debug("[UDLProtector] applyMapShield() called.");

        if (listenersField == null) {
            Udl.LOGGER.warn(
                    "[UDLProtector] Cannot apply Map Shield: listenersField is NULL."
            );
            return;
        }

        try {
            Object fieldValue = listenersField.get(MinecraftForge.EVENT_BUS);

//            Udl.LOGGER.debug(
//                    "[UDLProtector] Current listeners map type: {}",
//                    fieldValue != null
//                            ? fieldValue.getClass().getName()
//                            : "null"
//            );

            if (!(fieldValue instanceof ConcurrentHashMap)) {
                Udl.LOGGER.warn(
                        "[UDLProtector] Cannot apply Map Shield: listeners is not ConcurrentHashMap."
                );
                return;
            }

            ConcurrentHashMap<Object, Object> originalMap =
                    (ConcurrentHashMap<Object, Object>) fieldValue;

//            Udl.LOGGER.debug(
//                    "[UDLProtector] Current listeners map size={}",
//                    originalMap.size()
//            );

            if (originalMap instanceof ProtectedListenersMap) {
                Udl.LOGGER.debug(
                        "[UDLProtector] Map Shield already applied. Skipping replacement."
                );
                return;
            }

            Udl.LOGGER.info(
                    "[UDLProtector] Applying ProtectedListenersMap shield..."
            );

            ProtectedListenersMap shieldedMap =
                    new ProtectedListenersMap(
                            originalMap,
                            anonymousListener
                    );

            listenersField.set(
                    MinecraftForge.EVENT_BUS,
                    shieldedMap
            );

            Udl.LOGGER.info(
                    "[UDLProtector] SUCCESS: ProtectedListenersMap installed. originalSize={}, newSize={}",
                    originalMap.size(),
                    shieldedMap.size()
            );

        } catch (Exception e) {
            Udl.LOGGER.error(
                    "[UDLProtector] ERROR while applying Map Shield.",
                    e
            );
        }
    }

    private void startCounterThread() {
        Udl.LOGGER.info(
                "[UDLProtector] Starting anti-unregister counter thread..."
        );

        Thread thread = new Thread(() -> {

            Udl.LOGGER.info(
                    "[UDLProtector] Anti-unregister thread started. threadName={}, priority={}",
                    Thread.currentThread().getName(),
                    Thread.currentThread().getPriority()
            );

            long cycle = 0;

            while (running) {
                try {
                    Thread.sleep(1000);

                    cycle++;

//                    Udl.LOGGER.debug(
//                            "[UDLProtector] Protection cycle #{}",
//                            cycle
//                    );

                    registerSafely();
                    applyMapShield();

                } catch (InterruptedException e) {
                    Udl.LOGGER.warn(
                            "[UDLProtector] Anti-unregister thread interrupted. Stopping thread."
                    );

                    Thread.currentThread().interrupt();
                    break;

                } catch (Exception e) {
                    Udl.LOGGER.error(
                            "[UDLProtector] Unexpected exception in anti-unregister thread.",
                            e
                    );
                }
            }

            Udl.LOGGER.info(
                    "[UDLProtector] Anti-unregister thread stopped. totalCycles={}",
                    cycle
            );

        }, "UDL-AntiUnregister-Thread");

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);

        Udl.LOGGER.info(
                "[UDLProtector] Thread configured. name={}, priority={}, daemon={}",
                thread.getName(),
                thread.getPriority(),
                thread.isDaemon()
        );

        thread.start();

        Udl.LOGGER.info(
                "[UDLProtector] Anti-unregister thread start() called."
        );
    }

    private static class ProtectedListenersMap
            extends ConcurrentHashMap<Object, Object> {

        private final Object protectedKey;

        public ProtectedListenersMap(
                ConcurrentHashMap<Object, Object> original,
                Object protectedKey
        ) {
            super(original);

            this.protectedKey = protectedKey;

            Udl.LOGGER.info(
                    "[UDLProtector] ProtectedListenersMap created. originalSize={}, protectedKeyClass={}, protectedKeyIdentity={}",
                    original.size(),
                    protectedKey != null
                            ? protectedKey.getClass().getName()
                            : "null",
                    protectedKey != null
                            ? System.identityHashCode(protectedKey)
                            : 0
            );
        }

        @Override
        public Object remove(Object key) {

            Udl.LOGGER.debug(
                    "[UDLProtector] ProtectedListenersMap.remove() called. key={}, keyClass={}",
                    key,
                    key != null ? key.getClass().getName() : "null"
            );

            if (isProtected(key)) {

                Udl.LOGGER.warn(
                        "[UDLProtector] BLOCKED removal of protected listener/key! key={}, keyClass={}",
                        key,
                        key != null ? key.getClass().getName() : "null"
                );

                return this.get(key);
            }

            Udl.LOGGER.debug(
                    "[UDLProtector] Allowing removal of unprotected key. key={}",
                    key
            );

            return super.remove(key);
        }

        @Override
        public boolean remove(Object key, Object value) {

            Udl.LOGGER.debug(
                    "[UDLProtector] ProtectedListenersMap.remove(key,value) called. key={}, value={}",
                    key,
                    value
            );

            if (isProtected(key)) {

                Udl.LOGGER.warn(
                        "[UDLProtector] BLOCKED conditional removal of protected listener/key! key={}",
                        key
                );

                return false;
            }

            return super.remove(key, value);
        }

        private boolean isProtected(Object key) {

            if (key == null) {
                Udl.LOGGER.debug(
                        "[UDLProtector] isProtected(null) -> false"
                );
                return false;
            }

            // 完全一致
            if (key == protectedKey) {
                Udl.LOGGER.debug(
                        "[UDLProtector] Protected key detected by identity comparison. key={}",
                        key
                );
                return true;
            }

            // equals一致
            if (key.equals(protectedKey)) {
                Udl.LOGGER.debug(
                        "[UDLProtector] Protected key detected by equals(). key={}",
                        key
                );
                return true;
            }

            String name =
                    (key instanceof Class)
                            ? ((Class<?>) key).getName()
                            : key.getClass().getName();

            boolean isUdlClass =
                    name.contains("com.zenith.udl");

            if (isUdlClass) {
                Udl.LOGGER.debug(
                        "[UDLProtector] Protected UDL class detected. className={}",
                        name
                );
            }

            return isUdlClass;
        }
    }
}
