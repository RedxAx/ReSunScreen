package me.combimagnetron.sunscreen.neo.editor.project;

import me.combimagnetron.passport.util.data.Identifier;
import me.combimagnetron.passport.util.math.Vec2i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EditorThemeBuilderState {
    private Identifier asset;
    private Vec2i regionPosition = Vec2i.zero();
    private Vec2i regionSize = Vec2i.of(1, 1);
    private Vec2i regionAnchor;
    private String mode = "region";
    private int top = 1;
    private int bottom = 1;
    private int left = 1;
    private int right = 1;

    public @Nullable Identifier asset() {
        return asset;
    }

    public void asset(@NotNull Identifier asset, @NotNull Vec2i size) {
        if (asset.equals(this.asset)) return;
        this.asset = asset;
        regionPosition = Vec2i.zero();
        regionSize = Vec2i.of(Math.max(1, size.x()), Math.max(1, size.y()));
        top = Math.max(1, size.y() / 3);
        bottom = Math.max(1, size.y() / 3);
        left = Math.max(1, size.x() / 3);
        right = Math.max(1, size.x() / 3);
        regionAnchor = null;
    }

    public @NotNull Vec2i regionPosition() {
        return regionPosition;
    }

    public @NotNull Vec2i regionSize() {
        return regionSize;
    }

    public int top() {
        return top;
    }

    public int bottom() {
        return bottom;
    }

    public int left() {
        return left;
    }

    public int right() {
        return right;
    }

    public @NotNull String mode() {
        return mode;
    }

    public void mode(@NotNull String mode) {
        this.mode = mode;
        regionAnchor = null;
    }

    public void click(@NotNull Vec2i imagePosition, @NotNull Vec2i imageSize) {
        Vec2i point = Vec2i.of(Math.clamp(imagePosition.x(), 0, Math.max(0, imageSize.x() - 1)), Math.clamp(imagePosition.y(), 0, Math.max(0, imageSize.y() - 1)));
        if (mode.equals("region")) {
            if (regionAnchor == null) {
                regionAnchor = point;
                regionPosition = point;
                regionSize = Vec2i.of(1, 1);
                return;
            }
            int minX = Math.min(regionAnchor.x(), point.x());
            int minY = Math.min(regionAnchor.y(), point.y());
            int maxX = Math.max(regionAnchor.x(), point.x());
            int maxY = Math.max(regionAnchor.y(), point.y());
            regionPosition = Vec2i.of(minX, minY);
            regionSize = Vec2i.of(Math.max(1, maxX - minX + 1), Math.max(1, maxY - minY + 1));
            top = Math.clamp(top, 1, Math.max(1, regionSize.y() - 1));
            bottom = Math.clamp(bottom, 1, Math.max(1, regionSize.y() - top));
            left = Math.clamp(left, 1, Math.max(1, regionSize.x() - 1));
            right = Math.clamp(right, 1, Math.max(1, regionSize.x() - left));
            regionAnchor = null;
            return;
        }
        if (mode.equals("top")) top = Math.clamp(point.y() - regionPosition.y(), 1, Math.max(1, regionSize.y() - bottom));
        if (mode.equals("bottom")) bottom = Math.clamp(regionPosition.y() + regionSize.y() - point.y(), 1, Math.max(1, regionSize.y() - top));
        if (mode.equals("left")) left = Math.clamp(point.x() - regionPosition.x(), 1, Math.max(1, regionSize.x() - right));
        if (mode.equals("right")) right = Math.clamp(regionPosition.x() + regionSize.x() - point.x(), 1, Math.max(1, regionSize.x() - left));
    }
}
