package com.zenith.udl.renderblender.api.iface;

import com.zenith.udl.renderblender.api.client.model.PerspectiveModelState;
import com.zenith.udl.renderblender.api.client.util.TransformUtils;

public interface IToolTransform {
    
    default PerspectiveModelState getToolTransform() {
        return TransformUtils.DEFAULT_TOOL;
    }

}
