This is the [Kaida](https://github.com/NousResearch/kaida)-based backend for Nous Research's experimental LLM story generator. 

Please see [here](https://github.com/NousResearch/storywriter-frontend) for the frontend!

To run:

`./gradlew run`

For production:

`./gradlew shadowJar`

You must configure authentication keys for Anthropic and Fireworks to use the repository as presented. Please review `config/auth.example.yaml`.