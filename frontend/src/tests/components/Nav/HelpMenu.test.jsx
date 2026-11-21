import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import HelpMenu from "main/components/Nav/HelpMenu";

describe("HelpMenu tests", () => {
  test("renders the Help dropdown", () => {
    render(
      <MemoryRouter>
        <HelpMenu />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("appnavbar-help-dropdown")).toBeInTheDocument();
  });

  test("About link points to /about", async () => {
    render(
      <MemoryRouter>
        <HelpMenu />
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByText("Help"));

    const aboutLink = screen.getByRole("link", { name: "About" });
    expect(aboutLink).toHaveAttribute("href", "/about");
  });
});
