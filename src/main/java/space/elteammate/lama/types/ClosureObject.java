package space.elteammate.lama.types;

public record ClosureObject(
        LamaCallTarget callTarget,
        Object[] captures
) {}
