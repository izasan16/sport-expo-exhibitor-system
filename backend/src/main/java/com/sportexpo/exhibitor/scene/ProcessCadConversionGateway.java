package com.sportexpo.exhibitor.scene;

import org.springframework.stereotype.Component;

@Component
public class ProcessCadConversionGateway implements CadConversionGateway {
    @Override
    public void requestConversion(SceneSourceFile sourceFile, SceneConversionJob job) {
        // Task 2 only creates conversion jobs. External execution is added later.
    }
}
