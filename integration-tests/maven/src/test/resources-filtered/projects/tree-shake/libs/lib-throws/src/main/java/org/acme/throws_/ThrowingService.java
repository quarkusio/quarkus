package org.acme.throws_;

public class ThrowingService {
    public String doWork() throws CustomException {
        try {
            return doRiskyWork();
        } catch (CaughtException e) {
            return "caught";
        }
    }

    private String doRiskyWork() throws CaughtException {
        return "ok";
    }
}
