package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

public class SubResourceLocatorPlatformServiceImpl implements SubResourceLocatorPlatformServiceResource {
    @Override
    public SubResourceLocatorUserResource getUserService(String entity, String ticket, String userId) {
        return new SubResourceLocatorUserResource() {
            @Override
            public List<SubResourceLocatorOhaUserModel> getByNameSurname(@PathParam("name") String name,
                    @PathParam("surname") String surname) {
                return null;
            }

            @Override
            public SubResourceLocatorOhaUserModel getUserByMail(@PathParam("mail") String mail) {
                return null;
            }

            @Override
            public Boolean update(@PathParam("id") String id, @QueryParam("adaId") String adaId,
                    @QueryParam("name") String name, @QueryParam("surname") String surname,
                    @QueryParam("address") String address, @QueryParam("city") String city,
                    @QueryParam("country") String country, @QueryParam("zipcode") String zipcode,
                    @QueryParam("email") String email, @QueryParam("phone") String phone,
                    @QueryParam("phone") String timezone) {
                return null;
            }

            @Override
            public Boolean updatePassword(@PathParam("username") String username, List<String> passwords) {
                return null;
            }

            @Override
            public Boolean create(@QueryParam("email") String email, @QueryParam("password") String password,
                    @QueryParam("username") String username) {
                return null;
            }

            @Override
            public Boolean showHelp(@PathParam("user") long userId) {
                return null;
            }

            @Override
            public Boolean setShowHelp(@PathParam("user") long userId, @PathParam("show") boolean showHelp) {
                return null;
            }

            @Override
            public void createJabberAccounts() {
            }

            @Override
            public SubResourceLocatorOhaUserModel getContent(String id) {
                return null;
            }

            @Override
            public SubResourceLocatorOhaUserModel add(SubResourceLocatorOhaUserModel object) {
                return null;
            }

            @Override
            public List<SubResourceLocatorOhaUserModel> get() {
                return null;
            }

            @Override
            public SubResourceLocatorOhaUserModel update(SubResourceLocatorOhaUserModel object) {
                return null;
            }

            @Override
            public Boolean delete(String id) {
                return null;
            }

            @Override
            public SubResourceLocatorOhaUserModel getUserDataByAdaId(String adaId) {
                return new SubResourceLocatorOhaUserModel("bill");
            }
        };
    }
}
