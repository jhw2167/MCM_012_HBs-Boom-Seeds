package com.holybuckets.boomseed.client.render;

import com.holybuckets.boomseed.block.be.SunJumpFlowerBlockEntity;
import com.holybuckets.boomseed.client.model.SunJumpFlowerModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SunJumpFlowerRenderer extends GeoBlockRenderer<SunJumpFlowerBlockEntity> {

    public SunJumpFlowerRenderer() {
        super(new SunJumpFlowerModel());
    }

}
