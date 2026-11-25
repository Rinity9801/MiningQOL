#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D OutlineSampler;
uniform sampler2D HaloSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float INTENSITY = 2.5;

void main() {
    vec4 outline = texture(OutlineSampler, texCoord);
    vec4 halo = texture(HaloSampler, texCoord);
    fragColor = vec4(
        outline.rgb * outline.a + halo.rgb * halo.a * INTENSITY,
        outline.a);
}
