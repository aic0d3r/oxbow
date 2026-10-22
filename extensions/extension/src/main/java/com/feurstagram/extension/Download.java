package com.feurstagram.extension;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Oxbow downloads: adds Instagram's own "Download" item to every post menu and saves
 * the original photo or video.
 *
 * Instagram's Media model is a view over a native Pando tree (LiveTreeJNI), whose
 * fields are keyed by String.hashCode() of the API field name ("image_versions2",
 * "video_versions", "carousel_media"...). LiveTreeJNI and TreeJNI keep their real
 * names across releases, so this reads stable API field names instead of obfuscated
 * getters. The only network traffic is the CDN fetch of the file itself, handed to
 * the system DownloadManager (progress notification, no storage permission needed).
 */
public final class Download {

    private static final String TAG = "OxbowDownload";
    private static final String FOLDER = "Oxbow";

    private Download() {}

    /** Instagram's own "Download" string id; the patch rewrites this body with the real one. */
    static int labelRes() {
        return 0;
    }

    /**
     * Hook, called while Instagram builds a post's overflow options: adds its native DOWNLOAD
     * option (own icon, own label) through Instagram's own row adder, found by shape:
     * static (state, Option, creator, CharSequence, ArrayList, boolean).
     */
    public static void addMenuItem(Object creator, ArrayList<Object> options) {
        try {
            for (Method m : creator.getClass().getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 6 || p[2] != creator.getClass() || p[4] != ArrayList.class
                        || p[5] != boolean.class || !p[1].isEnum()
                        || !p[1].getName().endsWith("MediaOption$Option")) continue;
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object download = Enum.valueOf((Class) p[1], "DOWNLOAD");
                m.setAccessible(true);
                m.invoke(null, null, download, creator, label(creator), options, false);
                return;
            }
            Log.w(TAG, "menu row adder not found");
        } catch (Throwable t) {
            Log.w(TAG, "could not add the menu item", t);
        }
    }

    /**
     * Hook: Instagram shows only a fixed whitelist of options on posts that are not yours
     * (see more / see fewer / report...). Lets DOWNLOAD through, just above Report; the sheet
     * sorts its rows by this list's order.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> allowInMenu(List<Object> allowed) {
        try {
            if (allowed.isEmpty()) return allowed;
            Class option = ((Enum) allowed.get(0)).getDeclaringClass();
            Object download = Enum.valueOf(option, "DOWNLOAD");
            if (allowed.contains(download)) return allowed;
            List<Object> out = new ArrayList<>(allowed);
            int report = out.indexOf(Enum.valueOf(option, "REPORT"));
            out.add(report < 0 ? out.size() : report, download);
            return out;
        } catch (Throwable t) {
            return allowed;
        }
    }

    private static String label(Object creator) {
        try {
            for (Field f : creator.getClass().getDeclaredFields()) {
                if (!Context.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Context context = (Context) f.get(creator);
                if (context != null && labelRes() != 0) return context.getString(labelRes());
            }
        } catch (Throwable ignored) {
        }
        return "Download";
    }

    /** Hook: true when the tapped overflow option is Instagram's native DOWNLOAD. */
    public static boolean isDownload(Object option) {
        return option instanceof Enum && "DOWNLOAD".equals(((Enum<?>) option).name());
    }

    /**
     * Hook: saves the post. {@code index} is the carousel slide on screen, or -1
     * for "all slides". Never throws: a failure must not break Instagram's menu.
     */
    public static void download(Context context, Object media, int index) {
        try {
            Node root = Node.of(media);
            if (root == null) throw new IllegalStateException("no media tree");

            List<Node> items = new ArrayList<>();
            List<Node> carousel = carouselOf(media);
            if (!carousel.isEmpty()) {
                if (index >= 0 && index < carousel.size()) items.add(carousel.get(index));
                else items.addAll(carousel);
            } else {
                items.add(root);
            }

            String base = fileBase(root);
            int saved = 0;
            for (int i = 0; i < items.size(); i++) {
                String[] best = bestUrl(items.get(i));
                if (best == null) continue;
                String suffix = items.size() > 1 ? "_" + (i + 1) : (carousel.size() > 1 ? "_" + (index + 1) : "");
                enqueue(context, best[0], base + suffix + best[1], ".mp4".equals(best[1]));
                saved++;
            }
            if (saved == 0) throw new IllegalStateException("no downloadable url");
            toast(context, saved == 1 ? "Downloading…" : "Downloading " + saved + " items…");
        } catch (Throwable t) {
            Log.w(TAG, "download failed", t);
            toast(context, "Couldn't download this post");
        }
    }

    /** Name of Media's carousel_media getter (List of Media); the patch rewrites this body. */
    static String carouselGetter() {
        return null;
    }

    /**
     * Carousel slides as nodes. Read through Media's own getter: it wraps each slide in a
     * Media of its own, whereas slides read straight off the tree carry no field schema.
     */
    private static List<Node> carouselOf(Object media) throws Exception {
        List<Node> out = new ArrayList<>();
        if (carouselGetter() == null) return out;
        Object slides = media.getClass().getMethod(carouselGetter()).invoke(media);
        if (slides instanceof List) {
            for (Object slide : (List<?>) slides) {
                Node node = slide == null ? null : Node.of(slide);
                if (node != null) out.add(node);
            }
        }
        return out;
    }

    /** {url, extension} of the largest video, else the largest image. */
    static String[] bestUrl(Node item) {
        String video = largest(item.list("video_versions"));
        if (video != null) return new String[] {video, ".mp4"};
        Node images = item.tree("image_versions2");
        String image = images == null ? null : largest(images.list("candidates"));
        return image == null ? null : new String[] {image, ".jpg"};
    }

    private static String largest(List<Node> candidates) {
        String best = null;
        long bestArea = -1;
        for (Node c : candidates) {
            String url = c.string("url");
            long area = (long) c.integer("width") * c.integer("height");
            if (url != null && area > bestArea) {
                best = url;
                bestArea = area;
            }
        }
        return best;
    }

    /** "username_shortcode", safe for file names. */
    static String fileBase(Node root) {
        Node user = root.tree("user");
        String name = user == null ? null : user.string("username");
        String code = root.string("code");
        String base = (name == null ? "instagram" : name) + "_" + (code == null ? System.currentTimeMillis() : code);
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * One Pando node read by API field name: the Media's LiveTreeJNI at the root, TreeJNI
     * below it. Both key fields by String.hashCode(); only the getter names differ.
     */
    static final class Node {
        private final Object target;
        private final boolean live;
        private final Class<?> treeClass;

        private Node(Object target, boolean live, Class<?> treeClass) {
            this.target = target;
            this.live = live;
            this.treeClass = treeClass;
        }

        static Node of(Object media) throws Exception {
            for (Class<?> c = media.getClass(); c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!f.getType().getName().equals("com.instagram.pando.livetree.LiveTreeJNI")) continue;
                    f.setAccessible(true);
                    Object liveTree = f.get(media);
                    if (liveTree == null) continue;
                    Class<?> tree = Class.forName("com.facebook.pando.TreeJNI", false, media.getClass().getClassLoader());
                    return new Node(liveTree, true, tree);
                }
            }
            return null;
        }

        private Object call(String liveName, String treeName, String field, boolean withClass) {
            try {
                String name = live ? liveName : treeName;
                Method m = withClass
                        ? target.getClass().getMethod(name, int.class, Class.class)
                        : target.getClass().getMethod(name, int.class);
                return withClass ? m.invoke(target, field.hashCode(), treeClass) : m.invoke(target, field.hashCode());
            } catch (Throwable t) {
                return null; // missing field or getter: treated as absent
            }
        }

        Node tree(String field) {
            Object o = call("getOptionalTreeValueNative", "getTreeValueByHashCode", field, true);
            return o == null ? null : new Node(o, false, treeClass);
        }

        List<Node> list(String field) {
            List<Node> out = new ArrayList<>();
            Object o = call("getOptionalTreeListNative", "getOptionalTreeListByHashCode", field, true);
            if (o instanceof List) for (Object e : (List<?>) o) if (e != null) out.add(new Node(e, false, treeClass));
            return out;
        }

        String string(String field) {
            Object o = call("getOptionalStringValueNative", "getStringValueByHashCode", field, false);
            return o instanceof String ? (String) o : null;
        }

        int integer(String field) {
            Object o = call("getOptionalIntValueNative", "getOptionalIntValueByHashCode", field, false);
            return o instanceof Integer ? (Integer) o : 0;
        }
    }

    private static void enqueue(Context context, String url, String fileName, boolean video) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                        video ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES,
                        FOLDER + "/" + fileName);
        dm.enqueue(request);
    }

    private static void toast(Context context, String text) {
        try {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
