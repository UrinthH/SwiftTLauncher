package net.kdt.pojavlaunch.customcontrols;

import android.content.Context;
import android.view.KeyEvent;

import com.google.gson.JsonSyntaxException;
import com.movtery.zalithlauncher.R;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.glfw.CallbackBridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LayoutConverter {
    public static CustomControls loadAndConvertIfNecessary(Context ctx, String jsonPath) throws IOException, JsonSyntaxException {

        String jsonLayoutData = Tools.read(jsonPath);
        try {
            JSONObject layoutJobj = new JSONObject(jsonLayoutData);
            return loadAndConvertIfNecessary(ctx, layoutJobj, jsonLayoutData, jsonPath, true);
        } catch (Exception e) {
            Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e);
            return null;
        }
    }

    public static CustomControls loadFromAssets(Context context, String jsonName) {
        try (InputStream is = context.getAssets().open(jsonName)) {
            String string = IOUtils.toString(is, StandardCharsets.UTF_8);

            JSONObject layoutJobj = new JSONObject(string);
            return loadAndConvertIfNecessary(context, layoutJobj, string, null, true);
        } catch (Exception e) {
            Tools.showError(context, context.getString(R.string.controls_load_failed), e);
            return null;
        }
    }

    public static CustomControls loadFromJsonObject(Context ctx, JSONObject layoutJobj, String jsonString, String jsonPath, boolean showError) {
        try {
            return loadAndConvertIfNecessary(ctx, layoutJobj, jsonString, jsonPath, showError);
        } catch (Exception e) {
            if (showError) Tools.showError(ctx, ctx.getString(R.string.controls_load_failed), e);
            return null;
        }
    }

    private static CustomControls loadAndConvertIfNecessary(Context ctx, JSONObject layoutJobj, String jsonLayoutData, String jsonPath, boolean showError) throws Exception {
        if (!layoutJobj.has("version")) { //v1 layout
            CustomControls layout = LayoutConverter.convertV1Layout(layoutJobj);
            if (jsonPath != null) layout.save(jsonPath);
            return layout;
        } else {
            int version = layoutJobj.getInt("version");
            if (version == 2) {
                CustomControls layout = LayoutConverter.convertV2Layout(layoutJobj);
                if (jsonPath != null) layout.save(jsonPath);
                return layout;
            }
            if (version == 3 || version == 4 || version == 5) {
                return LayoutConverter.convertV3_4Layout(layoutJobj);
            }
            if (version == 6 || version == 7) {
                return convertV6_7Layout(layoutJobj);
            }
            if (version == 8 || version == 9) {
                CustomControls layout = Tools.GLOBAL_GSON.fromJson(jsonLayoutData, CustomControls.class);
                // Mojo Launcher v9 stores Android KeyEvent codes, while Zalith uses GLFW codes.
                if (version == 9) convertMojoV9Keycodes(layout);
                return layout;
            }
            if (showError) {
                IOException ioException = new IOException();
                Tools.showError(ctx, ctx.getString(R.string.controls_unsupported_layout_version), ioException);
            }
            return null;
        }
    }

    /**
     * Convert Android key codes exported by Mojo Launcher to GLFW key codes used by Zalith.
     * Negative values are Zalith's special virtual-button codes and zero means no key.
     */
    private static void convertMojoV9Keycodes(CustomControls layout) {
        for (ControlData data : layout.mControlDataList) convertMojoV9Keycodes(data);
        for (ControlDrawerData drawer : layout.mDrawerDataList) {
            convertMojoV9Keycodes(drawer.properties);
            for (ControlData data : drawer.buttonProperties) convertMojoV9Keycodes(data);
        }
        for (ControlJoystickData data : layout.mJoystickDataList) convertMojoV9Keycodes(data);
    }

    private static void convertMojoV9Keycodes(ControlData data) {
        if (data.keycodes == null) return;
        for (int i = 0; i < data.keycodes.length; i++) {
            data.keycodes[i] = androidToGlfwKeycode(data.keycodes[i]);
        }
    }

    private static int androidToGlfwKeycode(int keycode) {
        if (keycode <= 0) return keycode;
        if (keycode >= KeyEvent.KEYCODE_A && keycode <= KeyEvent.KEYCODE_Z) {
            return LwjglGlfwKeycode.GLFW_KEY_A + keycode - KeyEvent.KEYCODE_A;
        }
        if (keycode >= KeyEvent.KEYCODE_0 && keycode <= KeyEvent.KEYCODE_9) {
            return LwjglGlfwKeycode.GLFW_KEY_0 + keycode - KeyEvent.KEYCODE_0;
        }
        if (keycode >= KeyEvent.KEYCODE_F1 && keycode <= KeyEvent.KEYCODE_F12) {
            return LwjglGlfwKeycode.GLFW_KEY_F1 + keycode - KeyEvent.KEYCODE_F1;
        }
        if (keycode >= KeyEvent.KEYCODE_NUMPAD_0 && keycode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return LwjglGlfwKeycode.GLFW_KEY_KP_0 + keycode - KeyEvent.KEYCODE_NUMPAD_0;
        }
        switch (keycode) {
            case KeyEvent.KEYCODE_SPACE: return LwjglGlfwKeycode.GLFW_KEY_SPACE;
            case KeyEvent.KEYCODE_APOSTROPHE: return LwjglGlfwKeycode.GLFW_KEY_APOSTROPHE;
            case KeyEvent.KEYCODE_COMMA: return LwjglGlfwKeycode.GLFW_KEY_COMMA;
            case KeyEvent.KEYCODE_MINUS: return LwjglGlfwKeycode.GLFW_KEY_MINUS;
            case KeyEvent.KEYCODE_PERIOD: return LwjglGlfwKeycode.GLFW_KEY_PERIOD;
            case KeyEvent.KEYCODE_SLASH: return LwjglGlfwKeycode.GLFW_KEY_SLASH;
            case KeyEvent.KEYCODE_SEMICOLON: return LwjglGlfwKeycode.GLFW_KEY_SEMICOLON;
            case KeyEvent.KEYCODE_EQUALS: return LwjglGlfwKeycode.GLFW_KEY_EQUAL;
            case KeyEvent.KEYCODE_LEFT_BRACKET: return LwjglGlfwKeycode.GLFW_KEY_LEFT_BRACKET;
            case KeyEvent.KEYCODE_BACKSLASH: return LwjglGlfwKeycode.GLFW_KEY_BACKSLASH;
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_BRACKET;
            case KeyEvent.KEYCODE_GRAVE: return LwjglGlfwKeycode.GLFW_KEY_GRAVE_ACCENT;
            case KeyEvent.KEYCODE_ESCAPE: return LwjglGlfwKeycode.GLFW_KEY_ESCAPE;
            case KeyEvent.KEYCODE_ENTER: return LwjglGlfwKeycode.GLFW_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB: return LwjglGlfwKeycode.GLFW_KEY_TAB;
            case KeyEvent.KEYCODE_DEL: return LwjglGlfwKeycode.GLFW_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_FORWARD_DEL: return LwjglGlfwKeycode.GLFW_KEY_DELETE;
            case KeyEvent.KEYCODE_INSERT: return LwjglGlfwKeycode.GLFW_KEY_INSERT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_DOWN: return LwjglGlfwKeycode.GLFW_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP: return LwjglGlfwKeycode.GLFW_KEY_UP;
            case KeyEvent.KEYCODE_PAGE_UP: return LwjglGlfwKeycode.GLFW_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN: return LwjglGlfwKeycode.GLFW_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME: return LwjglGlfwKeycode.GLFW_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END: return LwjglGlfwKeycode.GLFW_KEY_END;
            case KeyEvent.KEYCODE_CAPS_LOCK: return LwjglGlfwKeycode.GLFW_KEY_CAPS_LOCK;
            case KeyEvent.KEYCODE_SCROLL_LOCK: return LwjglGlfwKeycode.GLFW_KEY_SCROLL_LOCK;
            case KeyEvent.KEYCODE_NUM_LOCK: return LwjglGlfwKeycode.GLFW_KEY_NUM_LOCK;
            case KeyEvent.KEYCODE_SYSRQ: return LwjglGlfwKeycode.GLFW_KEY_PRINT_SCREEN;
            case KeyEvent.KEYCODE_BREAK: return LwjglGlfwKeycode.GLFW_KEY_PAUSE;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
            case KeyEvent.KEYCODE_SHIFT_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_SHIFT;
            case KeyEvent.KEYCODE_CTRL_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_CONTROL;
            case KeyEvent.KEYCODE_ALT_LEFT: return LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT;
            case KeyEvent.KEYCODE_ALT_RIGHT: return LwjglGlfwKeycode.GLFW_KEY_RIGHT_ALT;
            case KeyEvent.KEYCODE_MENU: return LwjglGlfwKeycode.GLFW_KEY_MENU;
            default: return LwjglGlfwKeycode.GLFW_KEY_UNKNOWN;
        }
    }

    /**
     * Normalize the layout to v8 from v6/7. An issue from the joystick height and position has to be fixed.
     * @param oldLayoutJson The old layout
     * @return The new layout with the fixed joystick height
     */
    public static CustomControls convertV6_7Layout(JSONObject oldLayoutJson) {
        CustomControls layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls.class);
        for (ControlJoystickData data : layout.mJoystickDataList) {
            if (data.getHeight() > data.getWidth()) {
                // Make the size square, adjust the dynamic position related to height
                float ratio = data.getHeight() / data.getWidth();
                data.dynamicX = data.dynamicX.replace("${height}", "(" + ratio + " * ${height})");
                data.dynamicY = data.dynamicY.replace("${height}", "(" + ratio + " * ${height})") +  " + (" + (ratio-1) + " * ${height})";
                data.setHeight(data.getWidth());
            }
        }
        layout.version = 8;
        return layout;
    }

    /**
     * Normalize the layout to v6 from v3/4: The stroke width is no longer dependant on the button size
     */
    private static CustomControls convertV3_4Layout(JSONObject oldLayoutJson) {
        CustomControls layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls.class);
        convertStrokeWidth(layout);
        layout.version = 6;
        return layout;
    }


    private static CustomControls convertV2Layout(JSONObject oldLayoutJson) throws JSONException {
        CustomControls layout = Tools.GLOBAL_GSON.fromJson(oldLayoutJson.toString(), CustomControls.class);
        JSONArray layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList");
        layout.mControlDataList = new ArrayList<>(layoutMainArray.length());
        for (int i = 0; i < layoutMainArray.length(); i++) {
            JSONObject button = layoutMainArray.getJSONObject(i);
            ControlData n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlData.class);
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                double buttonC = button.getDouble("x");
                double ratio = buttonC / CallbackBridge.physicalWidth;
                n_button.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                double buttonC = button.getDouble("y");
                double ratio = buttonC / CallbackBridge.physicalHeight;
                n_button.dynamicY = ratio + " * ${screen_height}";
            }
            layout.mControlDataList.add(n_button);
        }
        JSONArray layoutDrawerArray = oldLayoutJson.getJSONArray("mDrawerDataList");
        layout.mDrawerDataList = new ArrayList<>();
        for (int i = 0; i < layoutDrawerArray.length(); i++) {
            JSONObject button = layoutDrawerArray.getJSONObject(i);
            JSONObject buttonProperties = button.getJSONObject("properties");
            ControlDrawerData n_button = Tools.GLOBAL_GSON.fromJson(button.toString(), ControlDrawerData.class);
            if (!Tools.isValidString(n_button.properties.dynamicX) && buttonProperties.has("x")) {
                double buttonC = buttonProperties.getDouble("x");
                double ratio = buttonC / CallbackBridge.physicalWidth;
                n_button.properties.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.properties.dynamicY) && buttonProperties.has("y")) {
                double buttonC = buttonProperties.getDouble("y");
                double ratio = buttonC / CallbackBridge.physicalHeight;
                n_button.properties.dynamicY = ratio + " * ${screen_height}";
            }
            layout.mDrawerDataList.add(n_button);
        }
        convertStrokeWidth(layout);

        layout.version = 3;
        return layout;
    }

    private static CustomControls convertV1Layout(JSONObject oldLayoutJson) throws JSONException {
        CustomControls empty = new CustomControls();
        JSONArray layoutMainArray = oldLayoutJson.getJSONArray("mControlDataList");
        for (int i = 0; i < layoutMainArray.length(); i++) {
            JSONObject button = layoutMainArray.getJSONObject(i);
            ControlData n_button = new ControlData();
            int[] keycodes = new int[]{LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN,
                    LwjglGlfwKeycode.GLFW_KEY_UNKNOWN};
            n_button.dynamicX = button.getString("dynamicX");
            n_button.dynamicY = button.getString("dynamicY");
            if (!Tools.isValidString(n_button.dynamicX) && button.has("x")) {
                double buttonC = button.getDouble("x");
                double ratio = buttonC / CallbackBridge.physicalWidth;
                n_button.dynamicX = ratio + " * ${screen_width}";
            }
            if (!Tools.isValidString(n_button.dynamicY) && button.has("y")) {
                double buttonC = button.getDouble("y");
                double ratio = buttonC / CallbackBridge.physicalHeight;
                n_button.dynamicY = ratio + " * ${screen_height}";
            }
            n_button.name = button.getString("name");
            n_button.opacity = ((float) ((button.getInt("transparency") - 100) * -1)) / 100f;
            n_button.passThruEnabled = button.getBoolean("passThruEnabled");
            n_button.isToggle = button.getBoolean("isToggle");
            n_button.setHeight(button.getInt("height"));
            n_button.setWidth(button.getInt("width"));
            n_button.bgColor = 0x4d000000;
            n_button.strokeWidth = 0;
            if (button.getBoolean("isRound")) {
                n_button.cornerRadius = 35f;
            }
            int next_idx = 0;
            if (button.getBoolean("holdShift")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
                next_idx++;
            }
            if (button.getBoolean("holdCtrl")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
                next_idx++;
            }
            if (button.getBoolean("holdAlt")) {
                keycodes[next_idx] = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT;
                next_idx++;
            }
            keycodes[next_idx] = button.getInt("keycode");
            n_button.keycodes = keycodes;
            empty.mControlDataList.add(n_button);
        }
        empty.scaledAt = (float) oldLayoutJson.getDouble("scaledAt");
        empty.version = 3;
        return empty;
    }


    /**
     * Convert the layout stroke width to the V5 form
     */
    private static void convertStrokeWidth(CustomControls layout) {
        for (ControlData data : layout.mControlDataList) {
            data.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.strokeWidth, data.getWidth(), data.getHeight()));
        }

        for (ControlDrawerData data : layout.mDrawerDataList) {
            data.properties.strokeWidth = Tools.pxToDp(computeStrokeWidth(data.properties.strokeWidth, data.properties.getWidth(), data.properties.getHeight()));
            for (ControlData subButtonData : data.buttonProperties) {
                subButtonData.strokeWidth = Tools.pxToDp(computeStrokeWidth(subButtonData.strokeWidth, data.properties.getWidth(), data.properties.getWidth()));
            }
        }
    }

    /**
     * Convert a size percentage into a px size, used by older layout versions
     */
    static int computeStrokeWidth(float widthInPercent, float width, float height) {
        float maxSize = Math.max(width, height);
        return (int) ((maxSize / 2) * (widthInPercent / 100));
    }
}
