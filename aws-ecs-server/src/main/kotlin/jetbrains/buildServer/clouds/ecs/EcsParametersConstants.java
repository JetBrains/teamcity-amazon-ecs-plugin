package jetbrains.buildServer.clouds.ecs;

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 06.07.17.
 */
public class EcsParametersConstants {

    private static final String SECRET_KEY = "ecs-secret-key";
    private static final String ACCESS_KEY_ID = "ecs-access-key-id";

    public String getAccessIdKey() {
        return ACCESS_KEY_ID;
    }

    public String getSecretKey() {
        return SECRET_KEY;
    }
}
