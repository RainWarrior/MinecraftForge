package net.minecraftforge.client.model.pipeline;

import net.minecraftforge.client.model.IFlexibleBakedModel;
import net.minecraftforge.client.model.IQuadInfo;

// interface for models that want to avoid serializing data into an array before passing it to the consumer
public interface IFastBakedModel extends IFlexibleBakedModel, IQuadInfo
{
    void pipe(IVertexConsumer consumer);
}
