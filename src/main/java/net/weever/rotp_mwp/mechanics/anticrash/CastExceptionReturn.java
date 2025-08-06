//package net.weever.rotp_mwp.init;
//
//import net.minecraftforge.fml.ModList;
//import net.weever.rotp_mwp.MobsWithPowersAddon;
//
//import java.util.List;
//
//public class CastExceptionReturn {
//    public static void setupGlobalHandler(List<String> handledMods, boolean remove) {
//        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
//            if (throwable instanceof ClassCastException && remove) {
//                handleException((ClassCastException) throwable, handledMods);
//            } else {
//                throw new RuntimeException(throwable);
//            }
//        });
//    }
//
//    private static void handleException(ClassCastException exception, List<String> handledMods) {
//        MobsWithPowersAddon.LOGGER.error("[CastExceptionReturn] ClassCastException: " + exception.getMessage());
//
//        for (StackTraceElement element : exception.getStackTrace()) {
//            String className = element.getClassName();
//
//            String modId = getModIdForClass(className);
//            if (modId != null && handledMods.contains(modId)) {
//                MobsWithPowersAddon.LOGGER.error("[CastExceptionReturn] Mod Crasher: " + modId);
//            }
//        }
//        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
//    }
//
//    private static String getModIdForClass(String className) {
//        return ModList.get().getMods().stream()
//                .filter(mod -> className.startsWith(mod.getOwningFile().getFile().getFileName()))
//                .map(mod -> mod.getModId())
//                .findFirst()
//                .orElse(null);
//    }
//}
