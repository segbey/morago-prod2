package com.morago.backend.service.pricing;

import com.morago.backend.entity.Call;
import java.math.BigDecimal;

public interface PricingService {
    BigDecimal computeCharge(Call call);
}
