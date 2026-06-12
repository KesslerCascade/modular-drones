package rearth.client.ui.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;
import rearth.drone.DroneData;

public record DroneGuiPreviewRenderState(
  DroneData droneData,
  float xRotation,
  float yRotation,
  int x0,
  int y0,
  int x1,
  int y1,
  float scale,
  @Nullable ScreenRectangle scissorArea,
  @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public DroneGuiPreviewRenderState(
      DroneData droneData,
      float xRotation,
      float yRotation,
      int x0,
      int y0,
      int x1,
      int y1,
      float scale,
      @Nullable ScreenRectangle scissorArea
    ) {
        this(droneData, xRotation, yRotation, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
