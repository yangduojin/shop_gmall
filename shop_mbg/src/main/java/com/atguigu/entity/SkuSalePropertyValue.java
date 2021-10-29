package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * sku销售属性值
 * </p>
 *
 * @author yx
 * @since 2021-10-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sku_sale_property_value")
@ApiModel(value="SkuSalePropertyValue对象", description="sku销售属性值")
public class SkuSalePropertyValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "库存单元id")
    private Long skuId;

    @ApiModelProperty(value = "spu_id(冗余)")
    private Long productId;

    @ApiModelProperty(value = "销售属性值id")
    private Long salePropertyValueId;


}
