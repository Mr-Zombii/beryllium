package me.zombii.beryllium.client.model.parts;

import dev.puzzleshq.puzzleloader.cosmic.game.util.HJsonSerializable;
import finalforeach.cosmicreach.util.constants.Direction;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Objects;

public class PartFace implements HJsonSerializable {
    private final int[] uv = new int[]{0, 0, 16, 16};
    private boolean useAO = true;
    private final int direction;
    private boolean isCulled = true;
    private int uvRotation;
    private int tintIndex = -1;
    private String textureID;

    public PartFace(PartFace face) {
        this(face.getDirection());
        this.textureID = face.textureID;
        this.isCulled = face.isCulled;
        this.uvRotation = face.uvRotation;
        this.tintIndex = face.tintIndex;
        this.useAO = face.useAO;
        System.arraycopy(face.uv, 0, this.uv, 0, face.uv.length);
    }

    public int uvHashCode() {
        return Objects.hashCode(uv);
    }

    public PartFace(Direction direction) {
        this.direction = direction.ordinal();
    }

    public PartFace(int direction) {
        if (direction < 0 || direction > 6) {
            throw new IllegalArgumentException("Invalid direction: " + direction);
        }
        this.direction = direction;
    }

    public int[] getUV() {
        return uv;
    }

    public int getDirection() {
        return direction;
    }

    public int getUVRotation() {
        return uvRotation;
    }

    public boolean isCulled() {
        return isCulled;
    }

    public String getTextureID() {
        return textureID;
    }

    public PartFace setCulled(boolean culled) {
        isCulled = culled;
        return this;
    }

    public PartFace setUVRotation(int uvRotation) {
        if ((((float)uvRotation) / 90) != (float)(uvRotation / 90))
            throw new IllegalArgumentException("UV Rotation must be divisible by 90");
        this.uvRotation = uvRotation;
        return this;
    }

    public PartFace setTextureID(String textureID) {
        this.textureID = textureID;
        return this;
    }

    public PartFace setTintIndex(int tintIndex) {
        this.tintIndex = tintIndex;
        return this;
    }

    public int getTintIndex() {
        return tintIndex;
    }

    public PartFace setAO(boolean ao) {
        this.useAO = ao;
        return this;
    }

    public boolean usesAO() {
        return useAO;
    }

    @Override
    public JsonValue toHJson() {
        JsonObject obj = new JsonObject();
        obj.set("uv", new JsonArray().add(uv[0]).add(uv[1]).add(uv[2]).add(uv[3]));
        obj.set("textureID", textureID);
        obj.set("doesCulling", isCulled);
        obj.set("uvRotation", uvRotation);
        obj.set("tintIndex", tintIndex);
        obj.set("aoEnabled", useAO);

        return obj;
    }
}