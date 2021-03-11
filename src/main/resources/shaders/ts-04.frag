#version 410 core

out vec4 o_color;

in vec3 va_position;
in float v_weight;

uniform vec4 color;
//in vec3 va_normal;
//in vec4 v_addedProperty;

void main() {
    float w = v_weight / 15.0;
//    o_color = vec4(1.0-w, 0.0, w, 1.0);
    o_color = color;
}