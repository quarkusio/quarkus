package org.jboss.resteasy.reactive.server.core.request;

import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * A individual variant quality bean for the RVSA (Remote Variant Selection Algorithm).
 *
 * @author Pascal S. de Kloe
 * @see "RFC 2296"
 */
public class VariantQuality {

    private QualityValue mediaTypeQualityValue = QualityValue.DEFAULT;
    private QualityValue characterSetQualityValue = QualityValue.DEFAULT;
    private QualityValue encodingQualityValue = QualityValue.DEFAULT;
    private QualityValue languageQualityValue = QualityValue.DEFAULT;

    private MediaType requestMediaType;

    public VariantQuality() {
    }

    public VariantQuality setMediaTypeQualityValue(QualityValue value) {
        if (value == null) {
            mediaTypeQualityValue = QualityValue.DEFAULT;
        } else {
            mediaTypeQualityValue = value;
        }
        return this;
    }

    public VariantQuality setCharacterSetQualityValue(QualityValue value) {
        if (value == null) {
            characterSetQualityValue = QualityValue.DEFAULT;
        } else {
            characterSetQualityValue = value;
        }
        return this;
    }

    public VariantQuality setEncodingQualityValue(QualityValue value) {
        if (value == null) {
            encodingQualityValue = QualityValue.DEFAULT;
        } else {
            encodingQualityValue = value;
        }
        return this;
    }

    public VariantQuality setLanguageQualityValue(QualityValue value) {
        if (value == null) {
            languageQualityValue = QualityValue.DEFAULT;
        } else {
            languageQualityValue = value;
        }
        return this;
    }

    public MediaType getRequestMediaType() {
        return requestMediaType;
    }

    public void setRequestMediaType(MediaType requestMediaType) {
        this.requestMediaType = requestMediaType;
    }

    /**
     * @return the quality value between zero and one with five decimal places after the point.
     * @see "3.3 Computing overall quality values"
     */
    public BigDecimal getOverallQuality() {
        BigDecimal qt = BigDecimal.valueOf(mediaTypeQualityValue.intValue(), 3);
        BigDecimal qc = BigDecimal.valueOf(characterSetQualityValue.intValue(), 3);
        BigDecimal qe = BigDecimal.valueOf(encodingQualityValue.intValue(), 3);
        BigDecimal ql = BigDecimal.valueOf(languageQualityValue.intValue(), 3);
        assert qt.compareTo(BigDecimal.ZERO) >= 0 && qt.compareTo(BigDecimal.ONE) <= 0;
        assert qc.compareTo(BigDecimal.ZERO) >= 0 && qc.compareTo(BigDecimal.ONE) <= 0;
        assert qe.compareTo(BigDecimal.ZERO) >= 0 && qe.compareTo(BigDecimal.ONE) <= 0;
        assert ql.compareTo(BigDecimal.ZERO) >= 0 && ql.compareTo(BigDecimal.ONE) <= 0;

        BigDecimal result = qt;
        result = result.multiply(qc, MathContext.DECIMAL32);
        result = result.multiply(qe, MathContext.DECIMAL32);
        result = result.multiply(ql, MathContext.DECIMAL32);
        assert result.compareTo(BigDecimal.ZERO) >= 0 && result.compareTo(BigDecimal.ONE) <= 0;

        long round5 = result.scaleByPowerOfTen(5).longValue();
        result = BigDecimal.valueOf(round5, 5);
        assert result.compareTo(BigDecimal.ZERO) >= 0 && result.compareTo(BigDecimal.ONE) <= 0;

        return result;
    }

}
