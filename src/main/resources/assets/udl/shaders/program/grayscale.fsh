#version 150

uniform sampler2D DiffuseSampler;
uniform float Progress; // Javaから送られてくる進捗率 (0.0 ～ 1.0)
uniform vec2 OutSize;   // 画面サイズ

in vec2 texCoord;
out vec4 fragColor;

// 簡単な擬似ランダムノイズ関数（波紋を少しガタガタさせる用）
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

// 2D ノイズ (スムーズな凸凹を作成)
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    vec2 uv = texCoord;

    // 綺麗な正円にするためアスペクト比補正
    vec2 aspect = vec2(OutSize.x / OutSize.y, 1.0);
    vec2 centeredUv = (uv - 0.5) * aspect;

    // --- 1. 一回だけ中央から広がって消える「水のようなガタガタ波紋」 ---
    // ノイズによって円周を少しガタガタ（波っぽく）させる
    float angle = atan(centeredUv.y, centeredUv.x);
    float distortion = (noise(vec2(angle * 2.5, Progress * 2.0)) - 0.5) * 0.08;

    // ガタガタを含めた中心からの距離
    float dist = length(centeredUv) + distortion;

    // 波の現在半径：Progress 0.0で中心(0.0)、1.0で画面外まで広がる(0.9)
    float waveRadius = Progress * 0.9;

    // 波の通過ラインからの距離（リングの形状）
    float ringDist = abs(dist - waveRadius);

    // 一回だけ発生し、途中でフェードアウト（前半〜中盤に強く表示され、後半消える）
    float waveLifetime = smoothstep(0.0, 0.1, Progress) * (1.0 - smoothstep(0.4, 0.85, Progress));

    // 透明な水の輪郭（レンズのような光の屈折歪み）
    float ringWidth = 0.04;
    float waveIntensity = smoothstep(ringWidth, 0.0, ringDist) * waveLifetime;

    // 波の進行方向に沿ってUVを少し歪ませる（水のような屈折効果）
    vec2 dir = normalize(centeredUv + 0.0001);
    vec2 distortedUv = uv + dir * waveIntensity * 0.25;


    // --- 2. 色収差 (Chromatic Aberration) ---
    float aberrationAmount = 0.04 * Progress * (length(centeredUv) * 1.5 + 0.2);

    vec4 colR = texture(DiffuseSampler, distortedUv + dir * aberrationAmount);
    vec4 colG = texture(DiffuseSampler, distortedUv);
    vec4 colB = texture(DiffuseSampler, distortedUv - dir * aberrationAmount);
    vec3 color = vec3(colR.r, colG.g, colB.b);


    // --- 3. 彩度の低下 ---
    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(color, vec3(gray), 0.3 * Progress);


    // --- 4. 画面端からの青紫ビネット侵食 ---
    float rawDist = length(centeredUv);
    // 画面端から中央に向かって侵食してくる度合い
    float vignetteEdge = smoothstep(0.9 - Progress * 0.55, 1.0, rawDist * 1.2);

    // 青紫色（グラデーション）
    vec3 bluePurpleTint = mix(vec3(0.1, 0.15, 0.55), vec3(0.45, 0.1, 0.65), rawDist);

    // 侵食処理
    color = mix(color, bluePurpleTint, vignetteEdge * 0.35 * Progress);


    // --- 5. 波紋の「水の光のハイライト」（水っぽさの強調） ---
    color += vec3(0.2, 0.25, 0.3) * waveIntensity * 1.0;

    fragColor = vec4(color, colG.a);
}