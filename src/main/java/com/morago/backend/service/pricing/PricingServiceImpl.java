package com.morago.backend.service.pricing;

import com.morago.backend.entity.Call;
import com.morago.backend.entity.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
public class PricingServiceImpl implements PricingService {
    @Override
    public BigDecimal computeCharge(Call call) {
        int duration = call.getDuration();
        BigDecimal commission = call.getCommission();
        return Money.s2(commission.multiply(BigDecimal.valueOf(duration)));
    }
}