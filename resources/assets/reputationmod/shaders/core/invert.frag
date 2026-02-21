#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord0);
    // Инвертируем RGB, альфа оставляем как есть
    fragColor = vec4(1.0 - color.rgb, color.a);
}