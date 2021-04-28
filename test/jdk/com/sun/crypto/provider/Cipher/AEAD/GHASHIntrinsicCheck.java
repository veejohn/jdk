/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Check GHASH intrinsic for correctness using byte[], heap byte
 * buffer and direct bytebuffer
 */

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

public class GHASHIntrinsicCheck {


    public static void main(String args[]) throws Exception {
        int i = 0;

        System.out.println("Running ByteArray test...");
        while (i++ < 1000000) {
            new GHASHByteArray().updateTest(15);
        }
        i = 0;
        System.out.println("Running ByteBuffer update test...");
        while (i++ < 1000000) {
            new GHASHByteBuffer().updateTest(15);
        }
    }

    static class GHASHByteArray extends GHASHIntrinsicCheck {
        GHASHByteArray() throws Exception {
            super();
        }

        void updateTest(int chunkLen) throws Exception {
            out = new ByteArrayOutputStream(expectedOut.length);
            int pos = 0;
            while (pos < in.length - AESBLOCK) {
                out.write(c.update(in, pos, chunkLen));
                pos += chunkLen;
            }
            out.write(c.doFinal(in, pos, in.length - pos));
            compare(out.toByteArray());
        }
    }

    static class GHASHByteBuffer extends GHASHIntrinsicCheck {
        ByteBuffer src = ByteBuffer.allocateDirect(in.length);
        ByteBuffer dst = ByteBuffer.allocateDirect(expectedOut.length);

        GHASHByteBuffer() throws Exception {
            super();
            src.put(in);
            src.flip();
        }

        void updateTest(int chunkLen) throws Exception {
            src.mark();
            int end = src.limit();
            src.limit(0);
            while (src.position() < end - AESBLOCK) {
                src.limit(src.limit() + chunkLen);
                c.update(src, dst);
            }

            src.limit(end);
            c.doFinal(src, dst);
            dst.flip();

            byte[] outarray = new byte[expectedOut.length];
            dst.get(outarray);
            compare(outarray);
            src.reset();
        }

        void doFinalTest() throws Exception {
            System.out.println("Running ByteBuffer doFinal test...");
            c.doFinal(src, dst);
        }
    }

    ByteArrayOutputStream out;
    Cipher c;
    GCMParameterSpec spec = new GCMParameterSpec(120, iv);  // 15byte tag

    GHASHIntrinsicCheck() throws Exception {
        c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, spec);
    }

    void compare(byte[] out) throws Exception {
        if (Arrays.compare(out, expectedOut) != 0) {
            String s = "Ciphertext mismatch:\nresult (len=" +
                out.length + "):\n" +
                hex.formatHex(out) +
                "\nexpected (len=" + expectedOut.length + "):\n" +
                hex.formatHex(expectedOut);
            System.err.println(s);
            throw new Exception(s);
        }
    }

    final static int AESBLOCK = 16;

    static HexFormat hex = HexFormat.of();
    SecretKeySpec key = new SecretKeySpec(
        hex.parseHex("272f16edb81a7abbea887357a58c1917"), "AES");
    static byte[] iv = hex.parseHex("794ec588176c703d3d2a7a07");
    static byte[] in = new byte[2075];
    static byte[] expectedOut = hex.parseHex(
                "15b461672153270e8ba1e6789f7641c5411f3e642abda731b6086f535c216457" +
                "e87305bc59a1ff1f7e1e0bbdf302b75549b136606c67d7e5f71277aeca4bc670" +
                "07a98f78e0cfa002ed183e62f07893ad31fe67aad1bb37e15b957a14d145f14f" +
                "7483d041f2c3612ad5033155984470bdfc64d18df73c2745d92f28461bb09832" +
                "33524811321ba87d213692825815dd13f528dba601a3c319cac6be9b48686c23" +
                "a0ce23d5062916ea8827bbb243f585e446131489e951354c8ab24661f625c02e" +
                "15536c5bb602244e98993ff745f3e523399b2059f0e062d8933fad2366e7e147" +
                "510a931282bb0e3f635efe7bf05b1dd715f95f5858261b00735224256b6b3e80" +
                "7364cb53ff6d4e88f928cf67ac70da127718a8a35542efbae9dd7567c818a074" +
                "9a0c74bd69014639f59768bc55056d1166ea5523e8c66f9d78d980beb8f0d83b" +
                "a9e2c5544b94dc3a1a4b6f0f95f897b010150e89ebcacf0daee3c2793d6501a0" +
                "b58b411de273dee987e8e8cf8bb29ef2e7f655b46b55fabf64c6a4295e0d080b" +
                "6a570ace90eb0fe0f5b5d878bdd90eddaa1150e4d5a6505b350aac814fe99615" +
                "317ecd0516a464c7904011ef5922409c0d65b1e43b69d7c3293a8f7d3e9fbee9" +
                "eb91ec0007a7d6f72e64deb675d459c5ba07dcfd58d08e6820b100465e6e04f0" +
                "663e310584a00d36d23699c1bffc6afa094c75184fc7cde7ad35909c0f49f2f3" +
                "fe1e6d745ab628d74ea56b757047de57ce18b4b3c71e8af31a6fac16189cb0a3" +
                "a97a1bea447042ce382fcf726560476d759c24d5c735525ea26a332c2094408e" +
                "671c7deb81d5505bbfd178f866a6f3a011b3cfdbe089b4957a790688028dfdf7" +
                "9a096b3853f9d0d6d3feef230c7f5f46ffbf7486ebdaca5804dc5bf9d202415e" +
                "e0d67b365c2f92a17ea740807e4f0b198b42b54f15faa9dff2c7c35d2cf8d72e" +
                "b8f8b18875a2e7b5c43d1e0aa5139c461e8153c7f632895aa46ffe2b134e6a0d" +
                "dfbf6a336e709adfe951bd52c4dfc7b07a15fb3888fc35b7e758922f87a104c4" +
                "563c5c7839cfe5a7edbdb97264a7c4ebc90367b10cbe09dbf2390767ad7afaa8" +
                "8fb46b39d3f55f216d2104e5cf040bf3d39b758bea28e2dbce576c808d17a8eb" +
                "e2fd183ef42a774e39119dff1f539efeb6ad15d889dfcb0d54d0d4d4cc03c8d9" +
                "aa6c9ebd157f5e7170183298d6a30ada8792dcf793d931e2a1eafccbc63c11c0" +
                "c5c5ed60837f30017d693ccb294df392a8066a0594a56954aea7b78a16e9a11f" +
                "4a8bc2104070a7319f5fab0d2c4ccad8ec5cd8f47c839179bfd54a7bf225d502" +
                "cd0a318752fe763e8c09eb88fa57fc5399ad1f797d0595c7b8afdd23f13603e9" +
                "6802192bb51433b7723f4e512bd4f799feb94b458e7f9792f5f9bd6733828f70" +
                "a6b7ffbbc0bb7575021f081ec2a0d37fecd7cda2daec9a3a9d9dfe1c8034cead" +
                "e4b56b581cc82bd5b74b2b30817967d9da33850336f171a4c68e2438e03f4b11" +
                "96da92f01b3b7aeab795180ccf40a4b090b1175a1fc0b67c95f93105c3aef00e" +
                "13d76cc402539192274fee703730cd0d1c5635257719cc96cacdbad00c6255e2" +
                "bd40c775b43ad09599e84f2c3205d75a6661ca3f151183be284b354ce21457d1" +
                "3ba65b9b2cdb81874bd14469c2008b3ddec78f7225ecc710cc70de7912ca6a6d" +
                "348168322ab59fdafcf5c833bfa0ad4046f4b6da90e9f263db7079af592eda07" +
                "5bf16c6b1a8346da9c292a48bf660860a4fc89eaef40bc132779938eca294569" +
                "787c740af2b5a8de7f5e10ac750d1e3d0ef3ed168ba408a676e10b8a20bd4be8" +
                "3e8336b45e54481726d73e1bd19f165a98e242aca0d8387f2dd22d02d74e23db" +
                "4cef9a523587413e0a44d7e3260019a34d3a6b38426ae9fa4655be338d721970" +
                "cb9fe76c073f26f9303093a033022cd2c62b2790bce633ba9026a1c93b6535f1" +
                "1882bf5880e511b9e1b0b7d8f23a993aae5fd275faac3a5b4ccaf7c06b0b266a" +
                "ee970a1e3a4cd7a41094f516960630534e692545b25a347c30e3f328bba4825f" +
                "ed754e5525d846131ecba7ca120a6aeabc7bab9f59c890c80b7e31f9bc741591" +
                "55d292433ce9558e104102f2cc63ee267c1c8333e841522707ea6d595cb802b9" +
                "61697da77bbc4cb404ea62570ab335ebffa2023730732ac5ddba1c3dbb5be408" +
                "3c50aea462c1ffa166d7cc3db4b742b747e81b452db2363e91374dee8c6b40f0" +
                "e7fbf50e60eaf5cc5649f6bb553aae772c185026ceb052af088c545330a1ffbf" +
                "50615b8c7247c6cd386afd7440654f4e15bcfae0c45442ec814fe88433a9d616" +
                "ee6cc3f163f0d3d325526d05f25d3b37ad5eeb3ca77248ad86c9042b16c65554" +
                "aebb6ad3e17b981492b13f42c5a5dc088e991da303e5a273fdbb8601aece4267" +
                "47b01f6cb972e6da1743a0d7866cf206e95f23c6f8e337c901b9cd34a9a1fbbe" +
                "1694f2c26b00dfa4d02c0d54540163e798fbdc9c25f30d6406f5b4c13f7ed619" +
                "34e350f4059c13aa5e973307a9e3058917cda96fdd082e9c629ccfb2a9f98d12" +
                "5c6e4703a7b0f348f5cdeb63cef2133d1c6c1a087591e0a2bca29d09c6565e66" +
                "e91042f83b0e74e60a5d57562c23e2fbcd6599c29d7c19e47cf625c2ce24bb8a" +
                "13f8e54041498437eec2cedd1e3d8e57a051baa962c0a62d70264d99c5ee716d" +
                "5c8b9078db08c8b2c5613f464198a7aff43f76c5b4612b46a4f1cd2a494386c5" +
                "7fd28f3d199f0ba8d8e39116cc7db16ce6188205ee49a9dce3d4fa32ea394919" +
                "f6e91ef58b84d00b99596b4306c2d9f432d917bb4ac73384c42ae12adb4920d8" +
                "c33a816febcb299dcddf3ec7a8eb6e04cdc90891c6e145bd9fc5f41dc4061a46" +
                "9feba38545b64ec8203f386ceef52785619e991d274ae80af7e54af535e0b011" +
                "5effdf847472992875e09398457604d04e0bb965db692c0cdcf11a" +
                // tag
                "687cc09c89298491deb51061d709af");


}
